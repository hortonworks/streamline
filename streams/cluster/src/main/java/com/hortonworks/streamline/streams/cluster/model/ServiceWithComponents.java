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
package com.hortonworks.streamline.streams.cluster.model;

import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;

import java.util.Collection;

public class ServiceWithComponents {
    private Service service;
    private Collection<ServiceConfiguration> configurations;
    private Collection<Component> components;

    public ServiceWithComponents(Service service) {
        this.service = service;
    }

    public Service getService() {
        return service;
    }

    public Collection<Component> getComponents() {
        return components;
    }

    public void setComponents(Collection<Component> components) {
        this.components = components;
    }

    public Collection<ServiceConfiguration> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Collection<ServiceConfiguration> configurations) {
        this.configurations = configurations;
    }
}