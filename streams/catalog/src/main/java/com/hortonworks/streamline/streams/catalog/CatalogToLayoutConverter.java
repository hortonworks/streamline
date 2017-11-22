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
package com.hortonworks.streamline.streams.catalog;

import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.layout.component.*;

import java.io.IOException;
import java.util.HashSet;

public final class CatalogToLayoutConverter {
    private CatalogToLayoutConverter() {
    }

    public static TopologyLayout getTopologyLayout(Topology topology) throws IOException {
        return new TopologyLayout(topology.getId(), topology.getName(),
                topology.getConfig(), null);
    }

    public static TopologyLayout getTopologyLayout(Topology topology, TopologyDag topologyDag) throws IOException {
        return new TopologyLayout(topology.getId(), topology.getName(),
                topology.getConfig(), topologyDag);
    }

    public static com.hortonworks.streamline.streams.layout.component.Component getComponentLayout(TopologyComponent component) {
        StreamlineComponent componentLayout;
        if (component instanceof TopologySource) {
            componentLayout = new StreamlineSource() {
                @Override
                public void accept(TopologyDagVisitor visitor) {
                    throw new UnsupportedOperationException("Not intended to be called here.");
                }
            };
        } else if (component instanceof TopologyProcessor) {
            componentLayout = new StreamlineProcessor() {
                @Override
                public void accept(TopologyDagVisitor visitor) {
                    throw new UnsupportedOperationException("Not intended to be called here.");
                }
            };
        } else if (component instanceof TopologySink) {
            componentLayout = new StreamlineSink() {
                @Override
                public void accept(TopologyDagVisitor visitor) {
                    throw new UnsupportedOperationException("Not intended to be called here.");
                }
            };
        } else {
            componentLayout = new StreamlineComponent() {
                @Override
                public void accept(TopologyDagVisitor visitor) {
                    throw new UnsupportedOperationException("Not intended to be called here.");
                }
            };
        }

        componentLayout.setId(component.getId().toString());
        componentLayout.setName(component.getName());
        return componentLayout;
    }
}