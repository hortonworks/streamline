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

package com.hortonworks.iotas.cache.view.datastore.phoenix;

import com.hortonworks.iotas.cache.view.datastore.AbstractDataStore;
import com.hortonworks.iotas.cache.view.datastore.DataStoreReader;
import com.hortonworks.iotas.cache.view.datastore.DataStoreWriter;

import java.util.Collection;
import java.util.Map;

//TODO Remove abstract and create Phoenix Implementation
public class PhoenixDataStore<K,V> extends AbstractDataStore<K,V>
        implements DataStoreReader<K,V>, DataStoreWriter<K,V> {

    public PhoenixDataStore(String nameSpace) {
        super(nameSpace);
    }

    public V read(K key) {
        return null;
    }

    public Map<K, V> readAll(Collection<? extends K> keys){
        return null;
    }

    public void write(K key, V val){

    }

    public void writeAll(Map<? extends K, ? extends V> entries){

    }

    public void delete(K key){

    }

    public void deleteAll(Collection<? extends K> keys){

    }
}
