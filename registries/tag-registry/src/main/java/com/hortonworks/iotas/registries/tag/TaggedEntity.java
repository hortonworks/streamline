/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hortonworks.iotas.registries.tag;

import com.hortonworks.iotas.storage.Storable;

public class TaggedEntity {
    private String namespace;
    private Long id;

    public TaggedEntity(String namespace, Long id) {
        this.namespace = namespace;
        this.id = id;
    }

    public TaggedEntity(Storable storable) {
        this.namespace = storable.getNameSpace();
        this.id = storable.getId();
    }

    // for jackson
    public TaggedEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaggedEntity entityId = (TaggedEntity) o;

        if (id != null ? !id.equals(entityId.id) : entityId.id != null) return false;
        return namespace != null ? namespace.equals(entityId.namespace) : entityId.namespace == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TaggedEntity{" +
                "id=" + id +
                ", namespace='" + namespace + '\'' +
                '}';
    }
}
