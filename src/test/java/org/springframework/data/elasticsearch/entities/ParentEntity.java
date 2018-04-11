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
public class ParentEntity {

   	public static class MyJoin{
   		private String name;
   		private String parent;

		public MyJoin() {
		}

		public MyJoin(String name, String parent) {
			this.name = name;
			this.parent = parent;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getParent() {
			return parent;
		}

		public void setParent(String parent) {
			this.parent = parent;
		}
	}

	public static final String INDEX = "parent-child";
	public static final String PARENT_TYPE = "parent";
	public static final String CHILD_TYPE = "child";

	@Id
	private String id;
	@Field(type = FieldType.text, store = true)
	private String name;
	@Field(type = FieldType.text, store = true)
	@Type
	private String type="parent";

	public String getType() {
		return type;
	}

	@Join(joinParents = "parent", joinChildren = "child", routingField = "parent")
	private MyJoin join;

	public MyJoin getJoin() {
		return join;
	}

	public ParentEntity() {
		this.join = new MyJoin("parent", null);
	}

	public ParentEntity(String id, String name) {
		this();
		this.id = id;
		this.name = name;
	}


	@JsonIgnore
	@InnerHits(path = "child")
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

	@Document(indexName = INDEX, type = ParentEntity.INDEX, typeV6 = ParentEntity.PARENT_TYPE, shards = 1, replicas = 0, refreshInterval = "-1")
	public static class ChildEntity {

		@Id
		private String id;
		@Field(type = FieldType.text, store = true)
		@Join(joinParents = "parent", joinChildren = "child", routingField = "parent")
		private MyJoin join;
		public MyJoin getJoin() {
			return join;
		}
		@Field(type = FieldType.text, store = true)
		private String name;
		@Field(type = FieldType.text, store = true)
		@Type
		private String type="child";

		public String getType() {
			return type;
		}

		public ChildEntity() {
		}

		public ChildEntity(String id, String parentId, String name) {
			this.id = id;
			this.join = new MyJoin("child", parentId);
			this.name = name;
		}

		@JsonIgnore
		@InnerHits(path = "parent")
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
			return new ToStringCreator(this).append("id", id).append("parentId", join.getParent()).append("name", name).toString();
		}
	}
}
