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
package com.hortonworks.streamline.streams.runtime;

import com.hortonworks.streamline.streams.StreamlineEvent;

import java.io.Serializable;
import java.util.List;

/**
 * Abstraction for transformations on StreamlineEvent
 */
public interface TransformRuntime extends Serializable {

    /**
     * Transforms an input {@link StreamlineEvent} and generates zero, one or
     * more events as a result.
     *
     * @param input the input StreamlineEvent
     * @return the list of events generated from the transformation
     */
    List<StreamlineEvent> execute(StreamlineEvent input);

}
