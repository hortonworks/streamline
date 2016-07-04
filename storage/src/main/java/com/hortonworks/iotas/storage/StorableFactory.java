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
package com.hortonworks.iotas.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class StorableFactory {
    private static final Logger LOG = LoggerFactory.getLogger(StorableFactory.class);

    private Map<String, Class<? extends Storable>> nameSpaceWithClass = new HashMap<>();

    public StorableFactory() {
    }

    public void addStorableClasses(Collection<String> storableClasses) {
        for (String storableClass : storableClasses) {
            try {
                Class<? extends Storable> clazz = (Class<? extends Storable>) Class.forName(storableClass);
                String nameSpace = clazz.newInstance().getNameSpace();

                LOG.info("Storable class [{}] is getting registered with namespace [{}]", storableClass, nameSpace);

                if(nameSpaceWithClass.containsKey(nameSpace)) {
                    throw new IllegalArgumentException("NameSpace ["+nameSpace+"] is already registered");
                }

                nameSpaceWithClass.put(nameSpace, clazz);
            } catch (ClassNotFoundException |InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Storable create(String nameSpace) {
        if (!nameSpaceWithClass.containsKey(nameSpace)) {
            throw new IllegalArgumentException("No factory supported with the given namespace: " + nameSpace);
        }

        try {
            return nameSpaceWithClass.get(nameSpace).newInstance();
        } catch (InstantiationException  | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
