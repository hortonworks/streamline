package org.apache.streamline.registries.model.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.streamline.common.Schema;
import org.apache.streamline.common.Schema.Field;
import org.apache.streamline.common.Schema.Type;
import org.apache.streamline.storage.PrimaryKey;
import org.apache.streamline.storage.catalog.AbstractStorable;

import javax.print.attribute.standard.MediaSize.NA;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by schendamaraikannan on 12/7/16.
 */
public final class ModelInfo extends AbstractStorable {
    private static final String NAME_SPACE = "models";
    private static final String ID = "id";
    private InputStream pmml;
    private Long id;
    private Long timestamp;
    private String modelName;
    private String pmmlFileName;

    @JsonIgnore
    public InputStream getPMML() {
        return pmml;
    }

    @JsonIgnore
    public void setPmml(InputStream pmml) {
        this.pmml = pmml;
    }

    public void setPmmlFileName(String pmmlFileName) {
        this.pmmlFileName = pmmlFileName;
    }

    public String getPmmlFileName() {
        return pmmlFileName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getNameSpace() {
        return NAME_SPACE;
    }

    public PrimaryKey getPrimaryKey() {
        Map<Field, Object> fieldObjectMap = new HashMap<>();
        fieldObjectMap.put(new Schema.Field(ID, Type.LONG), this.id);
        return new PrimaryKey(fieldObjectMap);
    }
}
