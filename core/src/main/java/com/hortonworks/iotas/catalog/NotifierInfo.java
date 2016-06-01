package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The notifier instance config stored in database.
 */
public class NotifierInfo extends AbstractStorable {
    public static final String NAMESPACE = "notifierinfo";

    public static final String ID = "id";
    public static final String NOTIFIER_NAME = "name";
    public static final String JARFILE_NAME = "jarFileName";
    public static final String CLASS_NAME = "className";
    public static final String PROPERTIES = "properties";
    public static final String PROPERTIES_DATA = "propertiesData";
    public static final String FIELD_VALUES = "fieldValues";
    public static final String FIELD_VALUES_DATA = "fieldValuesData";
    public static final String TIMESTAMP = "timestamp";

    private Long id;
    private String name;
    private String jarFileName;
    private String className;
    private Map<String, String> properties;
    private Map<String, String> fieldValues;
    private Long timestamp;

    /**
     * The primary key
     */
    public Long getId() {
        return id;
    }

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
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
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

    @Override
    public Schema getSchema() {
        return Schema.of(
                Schema.Field.of(ID, Schema.Type.LONG),
                Schema.Field.of(NOTIFIER_NAME, Schema.Type.STRING),
                Schema.Field.of(JARFILE_NAME, Schema.Type.STRING),
                Schema.Field.of(CLASS_NAME, Schema.Type.STRING),
                Schema.Field.of(PROPERTIES_DATA, Schema.Type.STRING),
                Schema.Field.of(FIELD_VALUES_DATA, Schema.Type.STRING),
                Schema.Field.of(TIMESTAMP, Schema.Type.LONG)
                );
    }

    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> values = super.toMap();
        values.remove(PROPERTIES);
        values.remove(FIELD_VALUES);
        try {
            values.put(PROPERTIES_DATA, getPropertiesData());
            values.put(FIELD_VALUES_DATA, getFieldValuesData());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NotifierInfo that = (NotifierInfo) o;

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
}
