package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.exception.ParserException;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.exception.StorageException;
import com.hortonworks.iotas.util.ReflectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
/**
 * Default implementations go here
 */

public abstract class AbstractStorable implements Storable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractStorable.class);

    @JsonIgnore
    public StorableKey getStorableKey() {
        return new StorableKey(getNameSpace(), getPrimaryKey());
    }

    /**
     * Default implementation that will read all the instance variable names using API and
     * get the value by calling getter method (POJO) convention on it.
     *
     * Sometimes for JDBC to work we need an extra layer of transformation , for example see the implementation
     * in {@code DataSource} which defines a field of type @{code Type} which is enum and not a primitive type as expected
     * by the JDBC layer, you can call this method and override the fields that needs transformation.
     * @return
     */
    public Map<String, Object> toMap() {
        Set<String> instanceVariableNames = ReflectionHelper.getFieldNamesToTypes(this.getClass()).keySet();
        Map<String, Object> fieldToVal = new HashMap<>();
        for(String fieldName : instanceVariableNames) {
            try {
                Object val = ReflectionHelper.invokeGetter(fieldName, this);
                fieldToVal.put(fieldName, val);
                if(LOG.isTraceEnabled()) {
                    LOG.trace("toMap: Adding fieldName {} = {} ", fieldName, val);
                }
            } catch (NoSuchMethodException|InvocationTargetException|IllegalAccessException e) {
                throw new StorageException(e);
            }
        }

        return fieldToVal;
    }

    /**
     * Default implementation that will read all the instance variable names and invoke setter.
     *
     * Same as the toMap() method you should override this method when a field's defined type is not a primitive.
     * @return
     */
    public Storable fromMap(Map<String, Object> map) {
        for(Map.Entry<String, Object> entry: map.entrySet()) {
            try {
                if(entry.getValue() != null) {
                    ReflectionHelper.invokeSetter(entry.getKey(), this, entry.getValue());
                }
            } catch (NoSuchMethodException|InvocationTargetException|IllegalAccessException e) {
                throw new StorageException(e);
            }
        }
        return this;
    }

    /**
     * Default implementation that will generate schema by reading all the field names in the class and use its
     * define type to convert to the Schema type.
     * @return
     */
    @JsonIgnore
    public Schema getSchema() {
        Map<String, Class> fieldNamesToTypes = ReflectionHelper.getFieldNamesToTypes(this.getClass());
        List<Schema.Field> fields = new ArrayList<>();

        for(Map.Entry<String, Class> entry : fieldNamesToTypes.entrySet()) {
            try {
                Object val = ReflectionHelper.invokeGetter(entry.getKey(), this);
                Schema.Type type = null;
                if(val != null) {
                    type = Schema.fromJavaType(val);
                } else {
                    type = Schema.fromJavaType(entry.getValue());
                }
                fields.add(new Schema.Field(entry.getKey(), type));
                if(LOG.isTraceEnabled()) {
                    LOG.trace("getSchema: Adding {} = {} ", entry.getKey(), type);
                }
            } catch (NoSuchMethodException|InvocationTargetException|IllegalAccessException|ParserException e) {
                throw new StorageException(e);
            }
        }

        return Schema.of(fields);
    }

    @Override
    public Long getId() {
        throw new UnsupportedOperationException("Not implemented");
    }

}
