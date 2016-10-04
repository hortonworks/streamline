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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableFactory;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.storage.exception.AlreadyExistsException;
import com.hortonworks.iotas.storage.exception.StorageException;
import org.apache.atlas.AtlasException;
import org.apache.atlas.typesystem.Referenceable;
import org.apache.atlas.typesystem.exception.EntityExistsException;
import org.apache.atlas.typesystem.types.AttributeDefinition;
import org.apache.atlas.typesystem.types.ClassType;
import org.apache.atlas.typesystem.types.DataTypes;
import org.apache.atlas.typesystem.types.HierarchicalTypeDefinition;
import org.apache.atlas.typesystem.types.IDataType;
import org.apache.atlas.typesystem.types.Multiplicity;
import org.apache.atlas.typesystem.types.utils.TypesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link StorageManager} implementation for Atlas.
 * <pre>
 * Main issues with the current support in Atlas
 * - does not seem to support composite keys
 * - could not find api to find entities with multiple attributes, maybe because it does not support composite unique keys
 * - can not update traits properties, trait looks to be immutable, not a 1-1 mapping with tags
 * - no support of getting entities for a given trait
 * - no support of hierarchical traits
 * - no support of sequence ids for entities
 * - when long is stored, it returns BigInt instead of Long
 * Most of these problems are solved in this class to have a workaround for now.
 * </pre>
 */
public class AtlasStorageManager implements StorageManager {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasStorageManager.class);

    private final StorableFactory storableFactory = new StorableFactory();
    private AtlasSequence atlasSequence;
    private AtlasMetadataService atlasMetadataService;

    public AtlasStorageManager() {
    }

    @Override
    public void init(Map<String, Object> properties) {
        this.atlasMetadataService = properties != null ? new AtlasMetadataService(properties) : new AtlasMetadataService();
        atlasSequence = new AtlasSequence(atlasMetadataService);
    }

    @Override
    public void add(Storable storable) throws StorageException {
        Preconditions.checkNotNull(storable, "Storable argument can not be null");

        final Storable existing = get(storable.getStorableKey());

        if (existing == null) {
            try {
                atlasMetadataService.createEntity(referenceOf(storable));
            } catch (EntityExistsException e) {
                LOG.error("Another instance exists with the same primary key [{}]", storable.getPrimaryKey(), e);
                throw new AlreadyExistsException("Another instance exists with the same primary key: " + storable.getPrimaryKey());
            } catch (Exception e) {
                throw new StorageException(e);
            }
        } else if (!existing.equals(storable)) {
            throw new AlreadyExistsException("Another instance with same id = " + storable.getPrimaryKey()
                    + " exists with different value in namespace " + storable.getNameSpace()
                    + " Consider using addOrUpdate method if you always want to overwrite.");
        }

    }

    private Referenceable referenceOf(Storable storable) {
        Map<String, Object> values = new HashMap<>();
        for (Map.Entry<String, Object> entry : storable.toMap().entrySet()) {
            if (entry.getValue() != null) {
                values.put(entry.getKey(), entry.getValue());
            }
        }
        return new Referenceable(storable.getNameSpace(), values);
    }

    @Override
    public <T extends Storable> T remove(StorableKey key) throws StorageException {
        Preconditions.checkNotNull(key, "StorableKey argument can not be null");

        Collection<Referenceable> instances = null;
        try {
            // returned instances does not have actual entity info, Atlas should fix this!!
            instances = atlasMetadataService.remove(key.getNameSpace(),
                                            atlasMetadataService.createAttributes(key.getPrimaryKey().getFieldsToVal()));
        } catch (AtlasException e) {
            throw new StorageException(e);
        }

        T removedEntity = null;
        if (instances != null && !instances.isEmpty()) {
            if (instances.size() > 1) {
                LOG.warn("Expected one entity with the given key: [{}] but returning first entry.", key);
            }
            removedEntity = (T) storableFactory.create(key.getNameSpace()).fromMap(instances.iterator().next().getValuesMap());
        }

        return removedEntity;
    }

    @Override
    public void addOrUpdate(Storable storable) throws StorageException {
        Preconditions.checkNotNull(storable, "Storable argument can not be null");

        try {
            String entity = atlasMetadataService.addOrUpdateEntity(referenceOf(storable), storable.getPrimaryKey().getFieldsToVal().keySet().iterator().next().getName());
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    @Override
    public <T extends Storable> T get(StorableKey key) throws StorageException {
        Preconditions.checkNotNull(key, "StorableKey argument can not be null");

        String nameSpace = key.getNameSpace();
        Map<Schema.Field, Object> fieldsToVal = key.getPrimaryKey().getFieldsToVal();
        Collection<Referenceable> entities = null;
        try {
            entities = atlasMetadataService.getEntities(nameSpace, atlasMetadataService.createAttributes(fieldsToVal));
        } catch (AtlasException e) {
            throw new StorageException(e);
        }
        LOG.debug("Number of entities found [{}] for key [{}]", entities.size(), key);

        return entities != null && !entities.isEmpty() ? (T) storableOf(nameSpace, entities.iterator().next()) : null;
    }

    private <T extends Storable> T storableOf(String nameSpace, Referenceable referenceable) {
        Preconditions.checkArgument(nameSpace != null, "nameSpace can not be null");

        return referenceable != null ? (T) storableFactory.create(nameSpace).fromMap(referenceable.getValuesMap()) : null;
    }

    @Override
    public <T extends Storable> Collection<T> find(final String namespace, List<QueryParam> queryParams) throws StorageException {
        Preconditions.checkNotNull(namespace, "namespace argument can not be null");

        Map<String, Object> attributes = null;
        if (queryParams == null || queryParams.isEmpty()) {
            attributes = Collections.emptyMap();
        } else {
            attributes = new HashMap<>();
            for (QueryParam queryParam : queryParams) {
                attributes.put(queryParam.getName(), queryParam.getValue());
            }
        }

        Collection<Referenceable> entities = null;
        try {
            entities = atlasMetadataService.getEntities(namespace, attributes);
        } catch (AtlasException e) {
            throw new StorageException(e);
        }

        return (entities == null || entities.isEmpty())
                ? Collections.<T>emptyList()
                // wrapping in ArrayList as the transformed collection does not implement equals/hashCode
                : new ArrayList<>(Collections2.transform(entities, new Function<Referenceable, T>() {
                    @Nullable
                    @Override
                    public T apply(@Nullable Referenceable referenceable) {
                        return storableOf(namespace, referenceable);
                    }
                }));
    }

    @Override
    public <T extends Storable> Collection<T> list(final String namespace) throws StorageException {
        return find(namespace, Collections.<QueryParam>emptyList());
    }

    @Override
    public void cleanup() throws StorageException {
        atlasMetadataService.cleanup();
    }

    @Override
    public Long nextId(String namespace) throws StorageException {
        return atlasSequence.nextId();
    }

    @Override
    public void registerStorables(Collection<Class<? extends Storable>> classes) throws StorageException {
        storableFactory.addStorableClasses(classes);
        //register types
        try {
            for (Class<? extends Storable> clazz : classes) {
                Storable storable = clazz.newInstance();
                registerStorable(storable);
            }
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    private void registerStorable(Storable storable) throws Exception {
        List<AttributeDefinition> attrDefs = new ArrayList<>();
        Schema schema = storable.getSchema();
        Set<Schema.Field> primaryKeyFields = storable.getPrimaryKey().getFieldsToVal().keySet();
        for (Schema.Field field : schema.getFields()) {
            Multiplicity multiplicity = field.isOptional() ? Multiplicity.OPTIONAL : Multiplicity.REQUIRED;
            AttributeDefinition attrDef =
                    new AttributeDefinition(field.getName(), toAtlasType(field.getType()).getName(),
                            multiplicity, false, primaryKeyFields.contains(field), true, null);
            attrDefs.add(attrDef);
        }
        HierarchicalTypeDefinition<ClassType> classTypeDef = TypesUtil.createClassTypeDef(storable.getNameSpace(), storable.getNameSpace(), null, attrDefs.toArray(attrDefs.toArray(new AttributeDefinition[0])));
        atlasMetadataService.registerType(classTypeDef);
    }

    private IDataType toAtlasType(Schema.Type type) {
        switch (type) {
            case BYTE:
                return DataTypes.BYTE_TYPE;
            case BOOLEAN:
                return DataTypes.BOOLEAN_TYPE;
            case SHORT:
                return DataTypes.SHORT_TYPE;
            case INTEGER:
                return DataTypes.INT_TYPE;
            case FLOAT:
                return DataTypes.FLOAT_TYPE;
            case LONG:
                return DataTypes.LONG_TYPE;
            case DOUBLE:
                return DataTypes.DOUBLE_TYPE;
            case STRING:
                return DataTypes.STRING_TYPE;
            default:
                throw new IllegalArgumentException("Given type [" + type + "] is not supported");
        }
    }
}
