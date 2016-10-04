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

import com.google.common.base.Preconditions;
import org.apache.atlas.AtlasException;
import org.apache.atlas.typesystem.Referenceable;
import org.apache.atlas.typesystem.types.AttributeDefinition;
import org.apache.atlas.typesystem.types.ClassType;
import org.apache.atlas.typesystem.types.DataTypes;
import org.apache.atlas.typesystem.types.HierarchicalTypeDefinition;
import org.apache.atlas.typesystem.types.IDataType;
import org.apache.atlas.typesystem.types.Multiplicity;
import org.apache.atlas.typesystem.types.utils.TypesUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

/**
 *
 */
public class AtlasMetadataServiceTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        // cleanup existing graphdb dir
        cleanupGraphDbDir();
    }

    public static void cleanupGraphDbDir() throws ConfigurationException, IOException {
        InputStream resourceAsStream = AtlasStorageManagerTest.class.getResourceAsStream("/atlas-application.properties");
        if (resourceAsStream == null) {
            return;
        }
        PropertiesConfiguration propsConfig = new PropertiesConfiguration();
        propsConfig.load(resourceAsStream);
        Configuration configuration = propsConfig.interpolatedConfiguration();

        removeDirectories(configuration.getString("atlas.graph.storage.directory"),
                configuration.getString("atlas.graph.index.search.directory"));
    }

    private static void removeDirectories(String... dirs) throws IOException {
        for (String dir : dirs) {
            if (dir != null && !dir.isEmpty()) {
                FileUtils.deleteDirectory(new File(dir));
            }
        }
    }

    @Test
    public void testDeviceInfoType() throws Exception {
        AtlasMetadataService atlasMetadataService = new AtlasMetadataService();
        atlasMetadataService.registerType(createDeviceInfoType());

        DeviceInfo deviceInfo = createDeviceInfo();

        String instanceId = atlasMetadataService.createEntity(toReferenceable(deviceInfo));
        Referenceable entity = atlasMetadataService.getEntity(instanceId);
        DeviceInfo storedDeviceInfo = fromReferenceable(entity);

        Assert.assertEquals(deviceInfo, storedDeviceInfo);

        String timestamp = "" + (Long.parseLong(deviceInfo.getTimestamp()) + 1000);
        entity.set(DeviceInfo.TIMESTAMP, timestamp);
        atlasMetadataService.addOrUpdateEntity(entity);
        Assert.assertEquals(atlasMetadataService.getEntity(instanceId).get(DeviceInfo.TIMESTAMP), timestamp);

        atlasMetadataService.remove(DeviceInfo.NAME_SPACE, Collections.<String, Object>singletonMap(DeviceInfo.XID, deviceInfo.getXid()));
        try {
            Referenceable deletedEntity = atlasMetadataService.getEntity(instanceId);
            Assert.fail("getEntity should have thrown an exception here.");
        } catch (AtlasException e) {
            //expecting an exception, ignore it.
        }
        Collection<Referenceable> entities = atlasMetadataService.getEntities(DeviceInfo.NAME_SPACE);
        Assert.assertTrue(entities.isEmpty());
    }

    private Referenceable toReferenceable(DeviceInfo deviceInfo) {
        return new Referenceable(deviceInfo.getNameSpace(), deviceInfo.toMap());
    }

    public static HierarchicalTypeDefinition<ClassType> createDeviceInfoType() {
        return TypesUtil.createClassTypeDef(
                DeviceInfo.NAME_SPACE, null,
                attrDef(DeviceInfo.NAME, DataTypes.STRING_TYPE),
                TypesUtil.createUniqueRequiredAttrDef(DeviceInfo.XID, DataTypes.STRING_TYPE),
                attrDef(DeviceInfo.TIMESTAMP, DataTypes.STRING_TYPE),
                attrDef(DeviceInfo.VERSION, DataTypes.STRING_TYPE)
        );
    }

    private static AttributeDefinition attrDef(String name, IDataType dT) {
        return attrDef(name, dT, Multiplicity.OPTIONAL, false, null);
    }

    private static AttributeDefinition attrDef(String name, IDataType dT, Multiplicity m, boolean isComposite,
                                               String reverseAttributeName) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(dT);
        return new AttributeDefinition(name, dT.getName(), m, isComposite, false, false, reverseAttributeName);
    }

    private DeviceInfo fromReferenceable(Referenceable referenceable) {
        DeviceInfo deviceInfo = new DeviceInfo();

        deviceInfo.setXid(referenceable.get(DeviceInfo.XID).toString());
        deviceInfo.setName(referenceable.get(DeviceInfo.NAME).toString());
        deviceInfo.setTimestamp(referenceable.get(DeviceInfo.TIMESTAMP).toString());
        deviceInfo.setVersion(referenceable.get(DeviceInfo.VERSION).toString());

        return deviceInfo;
    }

    protected static DeviceInfo createDeviceInfo() {
        DeviceInfo deviceInfo = new DeviceInfo();
        long t = System.currentTimeMillis();
        deviceInfo.setName("device-" + t);
        deviceInfo.setXid("" + t);
        deviceInfo.setVersion("" + new Random().nextInt() % 10L);
        deviceInfo.setTimestamp("" + t);

        return deviceInfo;
    }

    public static void main(String[] args) throws Exception {
        AtlasMetadataServiceTest atlasMetadataServiceTest = new AtlasMetadataServiceTest();
        atlasMetadataServiceTest.testDeviceInfoType();
    }


}
