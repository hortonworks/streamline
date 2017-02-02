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
package com.hortonworks.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.storage.annotation.StorableEntity;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The notifier instance config stored in database.
 */
@StorableEntity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Notifier extends AbstractStorable {
    public static final String NAMESPACE = "notifier";

    public static final String ID = "id";
    public static final String NOTIFIER_NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String JARFILE_NAME = "jarFileName";
    public static final String CLASS_NAME = "className";
    public static final String PROPERTIES = "properties";
    public static final String FIELD_VALUES = "fieldValues";
    public static final String TIMESTAMP = "timestamp";

    private Long id;
    private String name;
    private String description;
    private String jarFileName;
    private String className;
    private Map<String, String> properties;
    private Map<String, String> fieldValues;
    private Long timestamp;

    /**
     * The primary key
     */
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * The unique user configured name for this notifier
     * e.g. email_notifier_1
     * @return
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    /**
     * The file name of the jar to be loaded.
     */
    public String getJarFileName() {
        return jarFileName;
    }

    public void setJarFileName(String jarFileName) {
        this.jarFileName = jarFileName;
    }

    /**
     * The class name
     */
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }


    /**
     * Notifier properties that user has specified.
     * @return
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * User configured field values for the notifier.
     * @return
     */
    public Map<String, String> getFieldValues() {
        return fieldValues;
    }

    public void setFieldValues(Map<String, String> fieldValues) {
        this.fieldValues = fieldValues;
    }

    @JsonIgnore
    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field("id", Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    private String getPropertiesData() throws Exception {
        if(properties == null) {
            return "";
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(properties);
    }

    public void setPropertiesData(String propertiesData) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        this.properties = mapper.readValue(propertiesData, new TypeReference<Map<String, String>>() {});
    }

    private String getFieldValuesData() throws Exception {
        if(fieldValues == null) {
            return "";
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(fieldValues);
    }

    public void setFieldValuesData(String fieldValuesData) throws Exception {
        if(fieldValuesData == null || fieldValuesData.isEmpty()) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        fieldValues = mapper.readValue(fieldValuesData, new TypeReference<Map<String, String>>() {});
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> result = super.toMap();
        try {
            result.put(PROPERTIES, this.getMapAsString(properties));
            result.put(FIELD_VALUES, this.getMapAsString(fieldValues));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public Storable fromMap(Map<String, Object> map) {
        this.setName((String) map.get(NOTIFIER_NAME));
        this.setDescription((String) map.get(DESCRIPTION));
        this.setId((Long) map.get(ID));
        this.setJarFileName((String) map.get(JARFILE_NAME));
        this.setClassName((String) map.get(CLASS_NAME));
        this.setTimestamp((Long) map.get(TIMESTAMP));
        try {
            this.setProperties(this.getStringAsMap((String) map.get(PROPERTIES)));
            this.setFieldValues(this.getStringAsMap((String) map.get(FIELD_VALUES)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @JsonIgnore
    public Schema getSchema() {
        List<Schema.Field> fields = new ArrayList<>();
        fields.add(new Schema.Field(ID, Schema.Type.LONG));
        fields.add(new Schema.Field(NOTIFIER_NAME, Schema.Type.STRING));
        fields.add(new Schema.Field(DESCRIPTION, Schema.Type.STRING));
        fields.add(new Schema.Field(JARFILE_NAME, Schema.Type.STRING));
        fields.add(new Schema.Field(CLASS_NAME, Schema.Type.STRING));
        fields.add(new Schema.Field(TIMESTAMP, Schema.Type.LONG));
        fields.add(new Schema.Field(PROPERTIES, Schema.Type.STRING));
        fields.add(new Schema.Field(FIELD_VALUES, Schema.Type.STRING));
        return Schema.of(fields);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Notifier that = (Notifier) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "NotifierInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", jarFileName='" + jarFileName + '\'' +
                ", className='" + className + '\'' +
                ", properties=" + properties +
                ", fieldValues=" + fieldValues +
                ", timestamp=" + timestamp +
                "} " + super.toString();
    }

    private String getMapAsString (Map<String, String> map) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(map);
    }

    private Map<String, String> getStringAsMap (String mapString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(mapString, Map.class);
    }
}
