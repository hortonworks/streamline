package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;

import java.util.HashMap;
import java.util.Map;

/**
 * The notifier instance config stored in database.
 */
public class NotifierInfo extends AbstractStorable {
    public static final String NAMESPACE = "notifierinfo";

    public static final String ID = "id";
    public static final String NOTIFIER_NAME = "notifierName";
    public static final String JARFILE_NAME = "jarFileName";
    public static final String CLASS_NAME = "className";
    public static final String PROPERTIES = "properties";
    public static final String FIELD_VALUES = "fieldValues";
    public static final String TIMESTAMP = "timestamp";

    private Long id;
    private String notifierName;
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
    public String getNotifierName() {
        return notifierName;
    }

    public void setNotifierName(String notifierName) {
        this.notifierName = notifierName;
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
    public Schema getSchema() {
        return new Schema.SchemaBuilder()
                .fields(new Schema.Field(ID, Schema.Type.LONG),
                        new Schema.Field(NOTIFIER_NAME, Schema.Type.STRING),
                        new Schema.Field(JARFILE_NAME, Schema.Type.STRING),
                        new Schema.Field(CLASS_NAME, Schema.Type.STRING),
                        new Schema.Field(PROPERTIES, Schema.Type.NESTED),
                        new Schema.Field(FIELD_VALUES, Schema.Type.NESTED),
                        new Schema.Field(TIMESTAMP, Schema.Type.LONG)).build();
    }

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field("id", Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

}
