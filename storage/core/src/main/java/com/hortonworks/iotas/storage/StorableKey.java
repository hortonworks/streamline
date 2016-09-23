package com.hortonworks.iotas.storage;

/**
 * Created by hlouro on 8/6/15.
 */
public class StorableKey {
    private final PrimaryKey primaryKey;
    private final String nameSpace;

    public StorableKey(String nameSpace, PrimaryKey primaryKey) {
        this.primaryKey = primaryKey;
        this.nameSpace = nameSpace;
    }

    /**
     * @return primary key
     */
    public PrimaryKey getPrimaryKey() {
        return primaryKey;
    }

    /**
     * @return the namespace associated with this key
     */
    public String getNameSpace() {
        return nameSpace;
    }

    // TODO: apply some syntax formatting guidelines
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StorableKey that = (StorableKey) o;

        if (primaryKey != null ? !primaryKey.equals(that.primaryKey) : that.primaryKey != null) return false;
        return !(nameSpace != null ? !nameSpace.equals(that.nameSpace) : that.nameSpace != null);

    }

    @Override
    public int hashCode() {
        int result = primaryKey != null ? primaryKey.hashCode() : 0;
        result = 31 * result + (nameSpace != null ? nameSpace.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StorableKey{" +
                "primaryKey=" + primaryKey +
                ", nameSpace='" + nameSpace + '\'' +
                '}';
    }
}
