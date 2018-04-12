/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.elasticsearch.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.core.style.ToStringCreator;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.util.List;

/**
 * ParentEntity
 *
 * @author Philipp Jardas
 * @author Mohsin Husen
 */


@Document(indexName = ParentEntity.INDEX, type = ParentEntity.INDEX, typeV6 = ParentEntity.PARENT_TYPE, shards = 1, replicas = 0, refreshInterval = "-1")
@Join(name = "my-join", parent = ParentEntity.PARENT_TYPE, children = ParentEntity.CHILD_TYPE)
public class ParentEntity {

	public static final String INDEX = "parent-child";
	public static final String PARENT_TYPE = "myparent";
	public static final String CHILD_TYPE = "mychild";

	@Id
	private String id;
	@Field(type = FieldType.text, store = true)
	private String name;


	public ParentEntity() {}

	public ParentEntity(String id, String name) {
		this();
		this.id = id;
		this.name = name;
	}


	@JsonIgnore
	@InnerHits(path = ParentEntity.CHILD_TYPE)
	private List<ChildEntity> children;

	@JsonIgnore
	public List<ChildEntity> getChildren() {
		return children;
	}

	@JsonIgnore
	public void setChildren(List<ChildEntity> children) {
		this.children = children;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}



	@Override
	public String toString() {
		return new ToStringCreator(this).append("id", id).append("name", name).toString();
	}

	@Document(indexName = INDEX, type = ParentEntity.INDEX, typeV6 = ParentEntity.CHILD_TYPE, shards = 1, replicas = 0, refreshInterval = "-1")
	@Join(name = "my-join", parent = ParentEntity.PARENT_TYPE, children = ParentEntity.CHILD_TYPE)
	public static class ChildEntity {

		@Id
		private String id;
		@Field(type = FieldType.text, store = true)
		@Join
		private String parentId;
		@Field(type = FieldType.text, store = true)
		private String name;

		public ChildEntity() {
		}

		public ChildEntity(String id, String parentId, String name) {
			this.id = id;
			this.parentId = parentId;
			this.name = name;
		}

		@JsonIgnore
		@InnerHits(path = ParentEntity.PARENT_TYPE)
		private Object parent;
		@JsonIgnore
		public ParentEntity getParent() {
			return (ParentEntity)parent;
		}
		@JsonIgnore
		public void setParent(ParentEntity parent) {
			this.parent = parent;
		}

		public String getId() {
			return id;
		}


		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return new ToStringCreator(this).append("id", id).append("parentId", parentId).append("name", name).toString();
		}
	}
}
