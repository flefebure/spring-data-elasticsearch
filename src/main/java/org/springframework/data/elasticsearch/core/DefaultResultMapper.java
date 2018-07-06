/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;


import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Artur Konczak
 * @author Petar Tahchiev
 * @author Young Gu
 * @author Oliver Gierke
 */
public class DefaultResultMapper extends AbstractResultMapper {

	private MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;

	public DefaultResultMapper() {
		super(new DefaultEntityMapper());
	}

	public DefaultResultMapper(MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {
		super(new DefaultEntityMapper());
		this.mappingContext = mappingContext;
	}

	public DefaultResultMapper(EntityMapper entityMapper) {
		super(entityMapper);
	}

	public DefaultResultMapper(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext,
			EntityMapper entityMapper) {
		super(entityMapper);
		this.mappingContext = mappingContext;
	}

	@Override
	public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
		long totalHits = response.getHits().getTotalHits();
		List<T> results = new ArrayList<T>();
		for (SearchHit hit : response.getHits()) {
			if (hit != null) {
				T result = null;
				if (StringUtils.isNotBlank(hit.getSourceAsString())) {
					result = mapEntity(hit.getSourceAsString(), clazz);
				} else {
					result = mapEntity(hit.getFields().values(), clazz);
				}
				mapInnerHits(result, hit.getInnerHits(), clazz);
				setPersistentEntityId(result, hit.getId(), clazz);
				populateScriptFields(result, hit);
				results.add(result);
			}
		}

		AggregatedPage<T> page = new AggregatedPageImpl<>(results, pageable, totalHits, response.getAggregations(), response.getScrollId());

		page.setTimedOut(response.isTimedOut());

		return page;
	}

	private <T> void populateScriptFields(T result, SearchHit hit) {
		if (hit.getFields() != null && !hit.getFields().isEmpty() && result != null) {
			for (java.lang.reflect.Field field : result.getClass().getDeclaredFields()) {
				ScriptedField scriptedField = field.getAnnotation(ScriptedField.class);
				if (scriptedField != null) {
					if (!scriptedField.asMap()) {
						String name = scriptedField.name().isEmpty() ? field.getName() : scriptedField.name();
						DocumentField searchHitField = hit.getFields().get(name);
						if (searchHitField != null) {
							field.setAccessible(true);
							try {
								field.set(result, searchHitField.getValue());
							} catch (IllegalArgumentException e) {
								throw new ElasticsearchException("failed to set scripted field: " + name + " with value: "
										+ searchHitField.getValue(), e);
							} catch (IllegalAccessException e) {
								throw new ElasticsearchException("failed to access scripted field: " + name, e);
							}
						}
					}
					else {
						field.setAccessible(true);
						Map<String, Object> scriptFields = null;
						try {
							scriptFields = (Map<String, Object>)field.get(result);
						} catch (IllegalAccessException e) {
							throw new ElasticsearchException("failed to access scripted field: " + field.getName(), e);
						}
						for (String fieldName : hit.getFields().keySet()) {
							DocumentField searchHitField = hit.getFields().get(fieldName);
							if (searchHitField != null && searchHitField.getValue() != null) {
								scriptFields.put(fieldName, searchHitField.getValue() );
							}
						}

					}
				}
			}
		}
	}


	private <T> T mapEntity(Collection<DocumentField> values, Class<T> clazz) {
		return mapEntity(buildJSONFromFields(values), clazz);
	}

	private String buildJSONFromFields(Collection<DocumentField> values) {
		JsonFactory nodeFactory = new JsonFactory();
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			JsonGenerator generator = nodeFactory.createGenerator(stream, JsonEncoding.UTF8);
			generator.writeStartObject();
			for (DocumentField value : values) {
				if (value.getValues().size() > 1) {
					generator.writeArrayFieldStart(value.getName());
					for (Object val : value.getValues()) {
						generator.writeObject(val);
					}
					generator.writeEndArray();
				} else {
					generator.writeObjectField(value.getName(), value.getValue());
				}
			}
			generator.writeEndObject();
			generator.flush();
			return new String(stream.toByteArray(), Charset.forName("UTF-8"));
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public <T> T mapResult(GetResponse response, Class<T> clazz) {
		T result = mapEntity(response.getSourceAsString(), clazz);
		if (result != null) {
			setPersistentEntityId(result, response.getId(), clazz);
		}
		return result;
	}

	@Override
	public <T> LinkedList<T> mapResults(MultiGetResponse responses, Class<T> clazz) {
		LinkedList<T> list = new LinkedList<T>();
		for (MultiGetItemResponse response : responses.getResponses()) {
			if (!response.isFailed() && response.getResponse().isExists()) {
				T result = mapEntity(response.getResponse().getSourceAsString(), clazz);
				setPersistentEntityId(result, response.getResponse().getId(), clazz);
				list.add(result);
			}
		}
		return list;
	}

	private <T> void setPersistentEntityId(T result, String id, Class<T> clazz) {

		if (mappingContext != null && clazz.isAnnotationPresent(Document.class)) {

			ElasticsearchPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(clazz);
			PersistentProperty<?> idProperty = persistentEntity.getIdProperty();
			
			// Only deal with text because ES generated Ids are strings !
			if (idProperty != null && idProperty.getType().isAssignableFrom(String.class)) {
				persistentEntity.getPropertyAccessor(result).setProperty(idProperty, id);
			}
		}
	}

	private <T> void mapInnerHits(T result, Map<String, SearchHits> innerHits, Class clazz) {
		if (innerHits == null || mappingContext == null) return;
		ElasticsearchPersistentEntity persistentEntity = mappingContext.getPersistentEntity(clazz);
		if (persistentEntity == null || persistentEntity.innerHitsProperties() == null) return;
		for (String path : innerHits.keySet()) {
			ElasticsearchPersistentProperty innerHitProperty = null;
			for (Object k : persistentEntity.innerHitsProperties().keySet()) {
				String key = (String)k;
				if (key.endsWith("*")) {
					String prefix = key.substring(0, key.length()-1);
					if (path.startsWith(prefix)) {
						innerHitProperty = (ElasticsearchPersistentProperty) persistentEntity.innerHitsProperties().get(key);
					}
				}
				else if (key.equals(path))
					innerHitProperty = (ElasticsearchPersistentProperty) persistentEntity.innerHitsProperties().get(key);
			}

			if (innerHitProperty != null) {
				try {
					// On va chercher le type sur le getter car dans certains cas il peut etre utile que la propriété soit un Object
					Method getter = new PropertyDescriptor(innerHitProperty.getFieldName(), persistentEntity.getType()).getReadMethod();
					Class innerHitType = getter.getReturnType();

					Collection innerCollection = null;
					Class innerClass = null;
					Object innerObject = null;
					if (List.class.isAssignableFrom(innerHitType)) {
						innerCollection = (Collection)getter.invoke(result, new Object[]{});
						if (innerCollection == null) innerCollection = new ArrayList();
						innerClass = innerHitProperty.getTypeInformation().getComponentType().getType();
					} else if (Set.class.isAssignableFrom(innerHitType)) {
						innerCollection = (Collection)getter.invoke(result, new Object[]{});
						innerClass = innerHitProperty.getTypeInformation().getComponentType().getType();
						if (innerCollection == null) innerCollection = new HashSet();
					} else {
						innerClass = innerHitType;
					}

					SearchHits innerSearchHits = innerHits.get(path);
					for (SearchHit searchHit : innerSearchHits.getHits()) {
						innerObject = mapEntity(searchHit.getSourceAsString(), innerClass);
						if (innerCollection != null) {
							innerCollection.add(innerObject);
						} else
							break;
					}
					Method setter = new PropertyDescriptor(innerHitProperty.getFieldName(), persistentEntity.getType()).getWriteMethod();

					if (innerCollection != null && !innerCollection.isEmpty()) {
						setter.invoke(result, innerCollection);
					} else if (innerObject != null) {
						setter.invoke(result, innerObject);
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (IntrospectionException e) {
					e.printStackTrace();
				}

			}
		}
	}
}
