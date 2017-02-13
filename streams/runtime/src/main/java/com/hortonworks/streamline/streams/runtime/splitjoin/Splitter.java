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

package com.hortonworks.streamline.streams.runtime.splitjoin;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.Result;

import java.util.List;
import java.util.Set;

/**
 * Splitter class can be implemented to split the given input event into multiple events for the given output streams.
 *
 */
public interface Splitter {

    /**
     * Splits the given {@code inputEvent} in to multiple events for the given {@code outputStreams}
     *
     * @param inputEvent
     * @param outputStreams
     * @return List of Results which contain split events for the given input event.
     */
    List<Result> splitEvent(StreamlineEvent inputEvent, Set<String> outputStreams);
}
