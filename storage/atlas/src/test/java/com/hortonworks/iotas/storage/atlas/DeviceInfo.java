/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hortonworks.iotas.storage.atlas;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.catalog.AbstractStorable;

import java.util.Collections;

/**
 *
 */
public class DeviceInfo extends AbstractStorable {

    public static final String XID = "xid";
    public static final String NAME = "name";
    public static final String TIMESTAMP = "timestamp";
    public static final String VERSION = "version";
    public static final String NAME_SPACE = "tests.device_info";

    private String name;
    private String timestamp;
    private String version;
    private String xid;

    @Override
    public String getNameSpace() {
        return NAME_SPACE;
    }

    @Override
    public Schema getSchema() {
        return Schema.of(Schema.Field.of(XID, Schema.Type.STRING),
                Schema.Field.of(NAME, Schema.Type.STRING),
                Schema.Field.optional(VERSION, Schema.Type.STRING),
                Schema.Field.optional(TIMESTAMP, Schema.Type.STRING)
        );
    }

    @Override
    public PrimaryKey getPrimaryKey() {
        return new PrimaryKey(Collections.<Schema.Field, Object>singletonMap(Schema.Field.of(XID, Schema.Type.STRING), xid));
    }

    @Override
    public StorableKey getStorableKey() {
        return new StorableKey(NAME_SPACE, getPrimaryKey());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceInfo that = (DeviceInfo) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;
        return xid != null ? xid.equals(that.xid) : that.xid == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (xid != null ? xid.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "name='" + name + '\'' +
                ", timestamp=" + timestamp +
                ", version=" + version +
                ", xid=" + xid +
                '}';
    }
}
