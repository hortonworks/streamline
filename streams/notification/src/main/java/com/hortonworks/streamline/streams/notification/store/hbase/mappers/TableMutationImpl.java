/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/


package com.hortonworks.streamline.streams.notification.store.hbase.mappers;

import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The {@link TableMutation} implementation that holds the
 * table name and the rows to be inserted, updated or deleted.
 */
public class TableMutationImpl implements TableMutation {
    private final String tableName;
    private final List<Put> updates;
    private final List<Delete> deletes;

    public TableMutationImpl(String tableName, List<Put> updateRows, List<Delete> deletes) {
        this.tableName = tableName;
        this.updates = updateRows;
        this.deletes = deletes;
    }

    public TableMutationImpl(String tableName, List<Put> updates) {
        this(tableName, updates, new ArrayList<Delete>());
    }

    // The Arrays.asList() is wrapped in new ArrayList<>() since the list should be mutable (support remove).
    public TableMutationImpl(String tableName, Put update) {
        this(tableName, new ArrayList<>(Arrays.asList(update)), new ArrayList<Delete>());
    }

    public TableMutationImpl(String tableName, Delete delete) {
        this(tableName, new ArrayList<Put>(), new ArrayList<>(Arrays.asList(delete)));
    }


    public TableMutationImpl(String tableName, Put update, Delete delete) {
        this(tableName, new ArrayList<>(Arrays.asList(update)), new ArrayList<>(Arrays.asList(delete)));
    }

    @Override
    public String tableName() {
        return tableName;
    }

    @Override
    public List<Put> updates() {
        return updates;
    }

    @Override
    public List<Delete> deletes() {
        return deletes;
    }

    @Override
    public String toString() {
        return "TableMutationImpl{" +
                "tableName='" + tableName + '\'' +
                ", updates=" + updates +
                ", deletes=" + deletes +
                '}';
    }
}
