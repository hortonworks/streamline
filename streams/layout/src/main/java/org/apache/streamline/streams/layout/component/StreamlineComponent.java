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
package org.apache.streamline.streams.layout.component;

import org.apache.streamline.common.Config;

import java.util.Collections;
import java.util.Set;

/**
 * Any Streamline design time topology component (source, sink, processor etc)
 * inherits from {@link StreamlineComponent} to provide
 * the default implementation for the {@link Component} methods.
 */
public abstract class StreamlineComponent implements Component {
    private String id;
    private String topologyComponentBundleId;
    private String name;
    private Config config;
    private String transformationClass;

    public StreamlineComponent() {
        config = new Config();
    }

    public StreamlineComponent(StreamlineComponent other) {
        this.id = other.id;
        this.topologyComponentBundleId = other.topologyComponentBundleId;
        this.name = other.name;
        this.config = new Config(other.getConfig());
        this.transformationClass = other.transformationClass;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getTopologyComponentBundleId() {
        return topologyComponentBundleId;
    }

    public void setTopologyComponentBundleId(String topologyComponentBundleId) {
        this.topologyComponentBundleId = topologyComponentBundleId;
    }

    public String getTransformationClass() {
        return transformationClass;
    }

    public void setTransformationClass(String transformationClass) {
        this.transformationClass = transformationClass;
    }

    /**
     * Subclasses can override to return any extra jars they
     * want to be shipped with the topology.
     *
     * @return the set of extra jars.
     */
    public Set<String> getExtraJars() {
        return Collections.emptySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StreamlineComponent that = (StreamlineComponent) o;

        return id != null ? id.equals(that.id) : that.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "StreamlineComponent{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", config='" + config + '\'' +
                '}';
    }

}
