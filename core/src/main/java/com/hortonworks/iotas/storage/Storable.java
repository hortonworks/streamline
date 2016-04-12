package com.hortonworks.iotas.storage;

import com.hortonworks.iotas.common.Schema;

import java.util.Map;

/**
 * Represents any entity that can be stored by our storage layer.
 */
public interface Storable {
    //TODO: probably we can remove getNameSpace and getPrimaryKey since we now have getStorableKey.
    //TODO: Leaving it for now for discussion purposes, as well as not to break the client code
    /**
     * Storage namespace this can be translated to a jdbc table or zookeeper node or hbase table.
     * TODO: Namespace can be a first class entity, probably needs its own class.
     * @return
     */
    String getNameSpace();

    /**
     * TODO: Get a better name.
     *
     * The actual columns for this storable entity and its types.
     * @return
     */
    Schema getSchema();

    /**
     * Defines set of columns that uniquely identifies this storage entity.
     * This can be translated to a primary key of a table.
     * of fields.
     */
    PrimaryKey getPrimaryKey();

    // TODO: why have both primary key and storable key ?
    // TODO: add some docs
    StorableKey getStorableKey();

    /**
     * TODO: Following two methods are not needed if we assume all storable entities will have setters and getters
     * for all the fields defined in its schema using POJO conventions. For now its easier to deal with maps rather
     * then Reflection. Both the methods will be needed for stuff like RelationalDB or HBase where each column/field
     * will be stored rather then the whole storable instance being stored as a single blob (json/protobuf/thrift)
     * which might be the case for unstructured storage systems like HDFS, S3, Zookeeper.
     */

    /**
     * Converts this storable instance to a map.
     * @return
     */
    Map<String, Object> toMap();        //TODO: Make this map type safe

    /**
     * Converts the given map to a storable instance and returns that instance.
     * Could just be a static method but we want overriding behavior.
     * @param map
     * @return
     */
    Storable fromMap(Map<String, Object> map);

    /**
     * A unique Id to identify the storable.
     *
     * @return the id.
     */
    Long getId();

}
