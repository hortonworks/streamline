/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.streams.cluster.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.common.ComponentUISpecification;
import com.hortonworks.registries.storage.PrimaryKey;
import com.hortonworks.registries.storage.Storable;
import com.hortonworks.registries.storage.StorableKey;
import com.hortonworks.registries.storage.annotation.StorableEntity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@StorableEntity
public class ServiceBundle implements Storable {

    public static final ObjectMapper mapper = new ObjectMapper();

    public static final String NAME_SPACE = "service_bundle";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String REGISTER_CLASS = "registerClass";
    public static final String UI_SPECIFICATION = "serviceUISpecification";
    public static final String TIMESTAMP = "timestamp";

    /**
     * Unique id for a service bundle. This is the primary key
     */
    private Long id;

    /**
     * The service name. For e.g. STORM, KAFKA, HDFS, etc.
     */
    private String name;

    /**
     * Time recording the creation or last update of this instance
     */
    private Long timestamp;

    /**
     * Object that will be used by ui to elicit values from user for registering this service
     */
    private ComponentUISpecification serviceUISpecification;

    /**
     * A fully qualified class name that can help registering the service.
     */
    private String registerClass;

    @Override
    @JsonIgnore
    public String getNameSpace () {
        return NAME_SPACE;
    }

    @Override
    @JsonIgnore
    public Schema getSchema () {
        return Schema.of(
                new Schema.Field(ID, Schema.Type.LONG),
                new Schema.Field(NAME, Schema.Type.STRING),
                new Schema.Field(TIMESTAMP, Schema.Type.LONG),
                new Schema.Field(UI_SPECIFICATION, Schema.Type.STRING),
                Schema.Field.optional(REGISTER_CLASS, Schema.Type.STRING)
        );
    }

    @Override
    @JsonIgnore
    public PrimaryKey getPrimaryKey () {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field(ID, Schema.Type.LONG),
                this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @Override
    @JsonIgnore
    public StorableKey getStorableKey () {
        return new StorableKey(getNameSpace(), getPrimaryKey());
    }

    @Override
    public Map toMap () {
        String uiSpecification;
        try {
            uiSpecification = mapper.writeValueAsString(serviceUISpecification);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ID, id);
        map.put(NAME, name);
        map.put(TIMESTAMP, timestamp);
        map.put(UI_SPECIFICATION, uiSpecification);
        map.put(REGISTER_CLASS, registerClass);
        return map;
    }

    @Override
    public Storable fromMap (Map<String, Object> map) {
        id = (Long) map.get(ID);
        name = (String)  map.get(NAME);
        timestamp = (Long) map.get(TIMESTAMP);
        try {
            serviceUISpecification = mapper.readValue((String) map.get(UI_SPECIFICATION), ComponentUISpecification.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        registerClass = (String) map.get(REGISTER_CLASS);
        return this;
    }

    public Long getId () {
        return id;
    }

    public void setId (Long id) {
        this.id = id;
    }

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public Long getTimestamp () {
        return timestamp;
    }

    public void setTimestamp (Long timestamp) {
        this.timestamp = timestamp;
    }

    public ComponentUISpecification getServiceUISpecification() {
        return serviceUISpecification;
    }

    public void setServiceUISpecification(ComponentUISpecification serviceUISpecification) {
        this.serviceUISpecification = serviceUISpecification;
    }

    public String getRegisterClass() {
        return registerClass;
    }

    public void setRegisterClass(String registerClass) {
        this.registerClass = registerClass;
    }

    @Override
    public String toString () {
        return "TopologyComponentBundle{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", timestamp=" + timestamp +
                ", serviceUISpecification='" + serviceUISpecification + '\'' +
                ", registerClass='" + registerClass + '\'' +
                '}';
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceBundle that = (ServiceBundle) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
        if (serviceUISpecification != null ? !serviceUISpecification.equals(that.serviceUISpecification) : that.serviceUISpecification!= null)
            return false;
        return !(registerClass != null ? !registerClass.equals(that.registerClass) : that.registerClass != null);

    }

    @Override
    public int hashCode () {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (serviceUISpecification != null ? serviceUISpecification.hashCode() : 0);
        result = 31 * result + (registerClass != null ? registerClass.hashCode() : 0);
        return result;
    }


}

