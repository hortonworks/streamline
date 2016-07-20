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

import org.apache.atlas.typesystem.Referenceable;
import org.apache.atlas.typesystem.types.ClassType;
import org.apache.atlas.typesystem.types.DataTypes;
import org.apache.atlas.typesystem.types.HierarchicalTypeDefinition;
import org.apache.atlas.typesystem.types.utils.TypesUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Basic sequence implementation for Atlas
 */
public class AtlasSequence {
    private static final String ENTITY_TYPE = "__iotas.version";
    private static final String ID = "id";

    private final AtlasMetadataService atlasMetadataService;
    private AtomicLong currentId = new AtomicLong(0);

    public AtlasSequence(AtlasMetadataService atlasMetadataService) {
        this.atlasMetadataService = atlasMetadataService;
        try {
            init(atlasMetadataService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void init(AtlasMetadataService atlasMetadataService) throws Exception {
        atlasMetadataService.registerType(createType());
        Collection<Referenceable> entityList = atlasMetadataService.getEntities(ENTITY_TYPE);
        if (entityList != null && !entityList.isEmpty()) {
            Collection<Long> storedVersions = new ArrayList<>();
            for (Referenceable referenceable : entityList) {
                if (referenceable != null) {
                    Long version = Long.valueOf(referenceable.getValuesMap().get(ID).toString());
                    storedVersions.add(version);
                }
            }
            if (!storedVersions.isEmpty()) {
                currentId.set(Collections.max(storedVersions));
            }
        }
    }

    public Long nextId() {
        long currentVal = currentId.get();
        Referenceable referenceable = new Referenceable(ENTITY_TYPE, Collections.<String, Object>singletonMap(ID, currentId.incrementAndGet()));
        try {
            atlasMetadataService.createEntity(referenceable);
            atlasMetadataService.remove(ENTITY_TYPE, Collections.<String, Object>singletonMap(ID, currentVal));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return currentVal;
    }

    private static HierarchicalTypeDefinition<ClassType> createType() {
        return TypesUtil.createClassTypeDef(
                ENTITY_TYPE, null,
                TypesUtil.createUniqueRequiredAttrDef(ID, DataTypes.LONG_TYPE));
    }
}
