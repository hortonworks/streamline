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

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hortonworks.iotas.common.Schema;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasException;
import org.apache.atlas.RepositoryMetadataModule;
import org.apache.atlas.RequestContext;
import org.apache.atlas.services.MetadataService;
import org.apache.atlas.typesystem.ITypedReferenceableInstance;
import org.apache.atlas.typesystem.Referenceable;
import org.apache.atlas.typesystem.json.InstanceSerialization;
import org.apache.atlas.typesystem.json.TypesSerialization;
import org.apache.atlas.typesystem.types.HierarchicalTypeDefinition;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Metadata service for Atlas for CRUD operations on entities.
 */
public class AtlasMetadataService {
    private static final String ATLAS_CONFIG_DIR = "atlas.config.dir";
    private static final String ATLAS_CONF = "ATLAS_CONF";

    private static Logger log = LoggerFactory.getLogger(AtlasMetadataService.class);

    private Injector metadataModuleInjector;
    private final MetadataService metadataService;

    public AtlasMetadataService() {
        this(Collections.<String, Object>emptyMap());
    }

    public AtlasMetadataService(Map<String, Object> properties) {
        init(properties);
        metadataModuleInjector = Guice.createInjector(new RepositoryMetadataModule());
        metadataService = metadataModuleInjector.getInstance(MetadataService.class);
    }

    private void init(Map<String, Object> properties) {
        // atlas properties are taken from
        //  - System property with ATLAS_CONF dir and props are taken from atlas-application.properties
        //  - current classloader searched with /atlas-application.properties
        if (properties == null || properties.isEmpty()
                || !properties.containsKey(ATLAS_CONFIG_DIR)) {
            return;
        }

        String atlasConfigDir = (String) properties.get(ATLAS_CONFIG_DIR);
        log.info("Atlas config dir is configured with: [{}] ", atlasConfigDir);
        try {
            File file = Files.createFile(Paths.get(atlasConfigDir, "atlas-application.properties")).toFile();
            file.deleteOnExit();
            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));) {
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    log.info("Atlas property key:[{}] value:[{}]", key, value);
                    bufferedWriter.write(key);
                    bufferedWriter.write("=");
                    bufferedWriter.write(value.toString());
                    bufferedWriter.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.setProperty(ATLAS_CONF, atlasConfigDir);
        log.info("System property [{}] is set as [{}]", ATLAS_CONF, atlasConfigDir);
    }

    public void registerType(HierarchicalTypeDefinition<?> typeDef) throws Exception {

        if (metadataService.getTypeNamesList().contains(typeDef.typeName)) {
            log.info("Given type: [{}] is already registered.", typeDef.typeName);
            return;
        }

        final String typeDefJson = TypesSerialization.toJson(typeDef, false);
        final JSONObject type = metadataService.createType(typeDefJson);
        log.info("####### registered type [{}] ", type);
        final List<String> typeNamesList = metadataService.getTypeNamesList();
        log.debug("####### registered typeNames [{}] ", typeNamesList);
    }

    public Referenceable getEntity(String entityGuid) throws AtlasException {
        String entityDefinition = metadataService.getEntityDefinition(entityGuid);
        return entityDefinition != null ? InstanceSerialization.fromJsonReferenceable(entityDefinition, true) : null;
    }

    public String addOrUpdateEntity(Referenceable referenceable, String uniqueAttrId) throws AtlasException {

        log.debug("Updating new entity [{}]", referenceable);

        Object attrValue = referenceable.getValuesMap().get(uniqueAttrId);
        Collection<Referenceable> entities = getEntities(referenceable.getTypeName(), Collections.singletonMap(uniqueAttrId, attrValue));

        if (entities.isEmpty()) {
            Referenceable newEntity = new Referenceable(referenceable.getTypeName(), referenceable.getValuesMap());
            return createEntity(newEntity);
        }

        Referenceable entity = entities.iterator().next();
        entity.getValuesMap().clear();
        entity.getValuesMap().putAll(referenceable.getValuesMap());

        String entityJSON = InstanceSerialization.toJson(entity, true);
        AtlasClient.EntityResult entityResult = _updateEntities(entityJSON);
        log.debug("Updated instance for type [{}]", referenceable.getTypeName() + ", guids: " + entityResult.getUpdateEntities());

        List<String> result = null;
        List<String> createdEntities = entityResult.getCreatedEntities();
        if(createdEntities != null && !createdEntities.isEmpty()) {
            result = createdEntities;
        } else {
            result = entityResult.getUpdateEntities();
        }

        return result!= null && !result.isEmpty() ? result.get(0) : null;
    }

    public String addOrUpdateEntity(Referenceable referenceable) throws AtlasException {

        String entityJSON = InstanceSerialization.toJson(referenceable, true);
        log.debug("Updating new entity [{}]", entityJSON);

        AtlasClient.EntityResult entityResult = _updateEntities(entityJSON);
        log.debug("Updated instance for type [{}]", referenceable.getTypeName() + ", guids: " + entityResult.getUpdateEntities());

        // return the Id for created instance with guids
        return entityResult.getUpdateEntities().get(0);
    }

    public String createEntity(Referenceable referenceable) throws AtlasException {
        String entityJSON = InstanceSerialization.toJson(referenceable, true);
        log.debug("Creating new entity [{}]", entityJSON);

        List<String> guids = _createEntities(entityJSON);
        log.debug("Created instance for type [{}]", referenceable.getTypeName() + ", guids: " + guids);

        if(guids == null || guids.isEmpty()) {
            return null;
        }

        // return the Id for created instance with guids
        return guids.get(0);
    }

    private List<String> _createEntities(String entityJSON) throws AtlasException {
        try {
            return metadataService.createEntities(new JSONArray(Arrays.asList(entityJSON)).toString());
        } finally {
            RequestContext.clear();
        }
    }

    private AtlasClient.EntityResult _updateEntities(String entityJSON) throws AtlasException {
        try {
            return metadataService.updateEntities(new JSONArray(Arrays.asList(entityJSON)).toString());
        } finally {
            RequestContext.clear();
        }
    }

    private Collection<Referenceable> _deleteEntities(Collection<Referenceable> entities) throws AtlasException {
        try {

            final Map<String, Referenceable> idWithReferenceables = new HashMap<>();
            for (Referenceable entity : entities) {
                idWithReferenceables.put(entity.getId().id, entity);
            }
            AtlasClient.EntityResult entityResult = metadataService.deleteEntities(Lists.newArrayList(idWithReferenceables.keySet()));
            // entityResult contains only ids but not the actual entities.
            List<ITypedReferenceableInstance> deletedInstances = RequestContext.get().getDeletedEntities();

            Collection<Referenceable> deletedEntities = new ArrayList<>();
            for (ITypedReferenceableInstance deletedInstance : deletedInstances) {
                deletedEntities.add(idWithReferenceables.get(deletedInstance.getId().id));
            }

            return deletedEntities;
        } finally {
            RequestContext.clear();
        }
    }


    public Collection<Referenceable> getEntities(String type) throws AtlasException {
        // inefficient API from atlas, better to have a single call to get all instances for a given entity type.
        List<String> entityGuids = metadataService.getEntityList(type);
        Collection<Referenceable> entities = new ArrayList<>();
        for (String entityGuid : entityGuids) {
            entities.add(getEntity(entityGuid));
        }

        return entities;
    }

    public Collection<Referenceable> getEntities(String type, Map<String, Object> attributes) throws AtlasException {
        // currently atlas supports only finding by a single unique attribute.
        log.debug("Getting entity with type [{}] and attributes [{}]", type, attributes);
        checkAttributes(attributes);

        if (attributes.isEmpty()) {
            return getEntities(type);
        }

        List<Referenceable> referenceables = new ArrayList<>();
        List<String> entityGuids = metadataService.getEntityList(type);
        Set<Map.Entry<String, Object>> attributesSet = attributes.entrySet();
        for (String entityGuid : entityGuids) {
            Referenceable entity = getEntity(entityGuid);
            Set<Map.Entry<String, Object>> entityValuesSet = entity.getValuesMap().entrySet();

            if (containsAll(attributesSet, entityValuesSet)) {
                referenceables.add(entity);
            }
        }

        return referenceables;
    }

    public Collection<String> getEntityIds(String type, Map<String, Object> attributes) throws AtlasException {
        // currently atlas supports only finding by a single unique attribute.
        log.debug("Getting entity with type [{}] and attributes [{}]", type, attributes);
        checkAttributes(attributes);

        if (attributes.isEmpty()) {
            return metadataService.getEntityList(type);
        }

        List<String> resultantIds = new ArrayList<>();
        List<String> entityGuids = metadataService.getEntityList(type);
        Set<Map.Entry<String, Object>> attributesSet = attributes.entrySet();
        for (String entityGuid : entityGuids) {
            Referenceable entity = getEntity(entityGuid);
            Set<Map.Entry<String, Object>> entityValuesSet = entity.getValuesMap().entrySet();

            if (containsAll(attributesSet, entityValuesSet)) {
                resultantIds.add(entityGuid);
            }
        }

        return resultantIds;
    }

    public Collection<Referenceable> remove(String type, Map<String, Object> attributes) throws AtlasException {
        // currently atlas supports only deleting by a single unique attribute.
        log.debug("Deleting entities with type [{}] and attributes [{}]", type, attributes);
        checkAttributes(attributes);

        Collection<Referenceable> entities = getEntities(type, attributes);
        log.debug("Number of entities with type[{}] and attributes[{}]: [{}] ", type, attributes, entities.size());

        if (entities.isEmpty()) {
            return Collections.emptyList();
        }

        Collection<Referenceable> deletedEntities = _deleteEntities(entities);

        return deletedEntities;
    }

    private void checkAttributes(Map<String, Object> attributes) {
        if (attributes == null) {
            throw new IllegalArgumentException("Attributes must be non null");
        }
    }

    /**
     * Returns true if {@code subSet} is sub set of {@code superSet}
     *
     * @param subSet
     * @param superSet
     * @return
     */
    private <T> boolean containsAll(Set<T> subSet, Set<T> superSet) {
        for (Object elem : subSet) {
            if (!superSet.contains(elem)) {
                return false;
            }
        }
        return true;
    }


    public void cleanup() {

    }

    public Map<String, Object> createAttributes(Map<Schema.Field, Object> fieldsToVal) {
        Map<String, Object> attributes = new HashMap<>();
        for (Map.Entry<Schema.Field, Object> entry : fieldsToVal.entrySet()) {
            attributes.put(entry.getKey().getName(), entry.getValue());
        }
        return attributes;
    }

}
