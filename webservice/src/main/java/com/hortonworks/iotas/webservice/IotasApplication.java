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
package com.hortonworks.iotas.webservice;

import com.google.common.collect.Lists;
import com.hortonworks.iotas.storage.InMemoryStorageManager;
import com.hortonworks.iotas.storage.StorageManager;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.I0Itec.zkclient.ZkClient;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import java.util.List;

public class IotasApplication extends Application<IotasConfiguration> {

    public static void main(String[] args) throws Exception {
        new IotasApplication().run(args);
    }

    @Override
    public String getName() {
        return "IoTaS Web Service";
    }

    @Override
    public void initialize(Bootstrap<IotasConfiguration> bootstrap) {
        super.initialize(bootstrap);
    }

    @Override
    public void run(IotasConfiguration iotasConfiguration, Environment environment) throws Exception {
        // kafka producer shouldn't be starting as part of REST api.
        // KafkaProducerManager kafkaProducerManager = new KafkaProducerManager(iotasConfiguration);
        // environment.lifecycle().manage(kafkaProducerManager);

        // ZkClient zkClient = new ZkClient(iotasConfiguration.getZookeeperHost());

        // final FeedResource feedResource = new FeedResource(kafkaProducerManager.getProducer(), zkClient);
        // environment.jersey().register(feedResource);

        // TODO we should load the implementation based on configuration
        StorageManager manager = new InMemoryStorageManager();

        final FeedCatalogResource feedResource = new FeedCatalogResource(manager);
        final ParserInfoCatalogResource parserResource = new ParserInfoCatalogResource(manager, iotasConfiguration);
        final DataSourceCatalogResource dataSourceResource = new DataSourceCatalogResource(manager);
        final DeviceCatalogResource deviceResource = new DeviceCatalogResource(manager);

        List<Object> resources = Lists.newArrayList(feedResource, parserResource, dataSourceResource, deviceResource);
        for(Object resource : resources) {
            environment.jersey().register(resource);
        }
        environment.jersey().register(MultiPartFeature.class);
    }
}
