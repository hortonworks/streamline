package com.hortonworks.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.storage.annotation.SearchableField;
import com.hortonworks.streamline.storage.annotation.StorableEntity;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorableKey;

import java.util.HashMap;
import java.util.Map;

@StorableEntity
public class Template implements Storable {
    public static final String NAMESPACE = "template";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String ENGINEID = "engineId";
    public static final String CONFIG = "config";


    private Long id;

    @SearchableField
    private String name;

    @SearchableField
    private String descrption;

    private Long engineId;

    private String  config;

    public Template() {}

    public Template(Template other) {
        if (other != null) {
            setId(other.getId());
            setName(other.getName());
            setDescription(other.getDescription());
            setEngineId(other.getEngineId());
            setConfig(other.getConfig());
        }
    }


    @JsonIgnore
    public String getNameSpace () {
        return NAMESPACE;
    }

    @JsonIgnore
    public Schema getSchema () {
        return Schema.of(
                new Schema.Field(ID, Schema.Type.LONG),
                new Schema.Field(NAME, Schema.Type.STRING),
                new Schema.Field(DESCRIPTION, Schema.Type.STRING),
                new Schema.Field(ENGINEID, Schema.Type.LONG),
                new Schema.Field(CONFIG, Schema.Type.STRING)
        );
    }

    @JsonIgnore
    public PrimaryKey getPrimaryKey () {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field(ID, Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @JsonIgnore
    public StorableKey getStorableKey () {
        return new StorableKey(getNameSpace(), getPrimaryKey());
    }

    public Map toMap () {
        Map<String, Object> map = new HashMap<>();
        map.put(ID, this.id);
        map.put(NAME, this.name);
        map.put(DESCRIPTION, this.descrption);
        map.put(ENGINEID, this.engineId);
        map.put(CONFIG, this.config);
        return map;
    }

    public Template fromMap (Map<String, Object> map) {
        this.id = (Long) map.get(ID);
        this.name = (String) map.get(NAME);
        this.descrption = (String) map.get(DESCRIPTION);
        this.engineId = (Long) map.get(ENGINEID);
        this.config = (String)  map.get(CONFIG);
        return this;
    }

    public Long getId() {return id;}

    public void setId(Long id) {this.id = id;}

    public String getName() {return name;}

    public void setName(String name) { this.name = name; }

    public String getDescription() { return descrption; }

    public void setDescription(String description) { this.descrption = description; }

    public Long getEngineId() { return engineId; }

    public void setEngineId(Long engineId) { this.engineId = engineId; }

    public String getConfig() { return config; }

    public void setConfig(String config) { this.config = config; }


}
