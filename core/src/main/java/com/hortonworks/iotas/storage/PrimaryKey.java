package com.hortonworks.iotas.storage;

import com.hortonworks.iotas.common.Schema;

import java.util.Map;

/**
 * An instance of this class represents what fields defines the primary key columns of a {@code Storable} entity.
 */
public class PrimaryKey {

    /**
     * The fieldsToVal map has {@code Schema.Field} as the key which defines the name of columns and their types that forms
     * the primary key. The value, if not null represents the actual value of that column in a stored instance. for exmaple
     * if you have a storable entity called "Employee" with primaryKey as "employeeId" an instance of this class
     * <pre>
     *     PrimaryKey {
     *         fieldsToVal = {
     *              new Field("employeeId", Field.Type.String) -> 1;
     *         }
     *     }
     * </pre>
     *
     * represents that employeeId is the primary key with type String and the value is actually referring to the row with
     * empolyeeId = 1.
     */
    private Map<Schema.Field, Object> fieldsToVal;

    public PrimaryKey(Map<Schema.Field, Object> fieldsToVal) {
        this.fieldsToVal = fieldsToVal;
    }

    @Override
    public String toString() {
        return "StorableId{" +
                "fieldsToVal=" + fieldsToVal +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrimaryKey)) return false;

        PrimaryKey that = (PrimaryKey) o;

        return fieldsToVal.equals(that.fieldsToVal);

    }

    @Override
    public int hashCode() {
        return fieldsToVal.hashCode();
    }

    public Map<Schema.Field, Object> getFieldsToVal() {
        return fieldsToVal;
    }
}
