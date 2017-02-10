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

package com.hortonworks.streamline.streams.layout.component;

import com.hortonworks.streamline.common.Config;

import java.io.Serializable;

/**
 * A component in the topology DAG. A component could be
 * a source, sink or a processor
 */
public interface Component extends TopologyDagComponent, Serializable {
    /**
     * Unique id of the component
     */
    String getId();

    /**
     * Component name
     */
    String getName();

    /**
     * Component config. This could be runtime specific (Storm/Spark etc)
     * and will be interpreted during runtime topology construction time to
     * construct the runtime components.
     */
    Config getConfig();
}
