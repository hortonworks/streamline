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
package com.hortonworks.streamline.streams.actions;

/**
 * Context information that can be passed to the topology actions and for topology
 * actions to pass status back to the framework.
 */
public interface TopologyActionContext {
    /**
     * Set the current action thats is being performed. This may be used to show the details
     * of the current step being executed.
     *
     * @param description the description
     */
    void setCurrentAction(String description);
}
