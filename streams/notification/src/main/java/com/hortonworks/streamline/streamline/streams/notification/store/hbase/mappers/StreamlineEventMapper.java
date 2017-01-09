/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.streamline.streams.notification.store.hbase.mappers;

import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.common.StreamlineEventImpl;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A mapper for the StreamlineEvent
 */
public class StreamlineEventMapper implements Mapper<StreamlineEvent> {

    // TODO: the table should be changed to "StreamlineEvent"
    private static final String TABLE_NAME = "nest";

    private static final byte[] CF_FIELDS = "cf".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CF_DATASOURCE_ID = "d".getBytes(StandardCharsets.UTF_8);

    @Override
    public List<TableMutation> tableMutations(StreamlineEvent event) {
        throw new UnsupportedOperationException("Not implemented, StreamlineEvent are currently inserted via HbaseBolt.");
    }

    @Override
    public StreamlineEvent entity(Result result) {
        String id = Bytes.toString(result.getRow());
        Map<String, Object> fieldsAndValues = new HashMap<>();
        for(Map.Entry<byte[], byte[]> entry: result.getFamilyMap(CF_FIELDS).entrySet()) {
            fieldsAndValues.put(Bytes.toString(entry.getKey()), Bytes.toString(entry.getValue()));
        }
        String dataSourceId = Bytes.toString(result.getFamilyMap(CF_DATASOURCE_ID).firstEntry().getKey());
        return new StreamlineEventImpl(fieldsAndValues, dataSourceId, id);
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public List<byte[]> mapMemberValue(String memberName, String value) {
        // Does not support querying by field.
        return null;
    }
}
