package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;

import java.util.Map;

/**
 * Default implementations go here
 */

public abstract class AbstractStorable implements Storable {

    @JsonIgnore
    public StorableKey getStorableKey() {
        return new StorableKey(getNameSpace(), getPrimaryKey());
    }

    public Map toMap() {
        throw new UnsupportedOperationException("toMap not implemented");
    }

    public Storable fromMap(Map<String, Object> map) {
        throw new UnsupportedOperationException("fromMap not implemented");
    }
}
