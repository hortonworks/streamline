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
package com.hortonworks.streamline.streams;

import java.util.List;

/**
 * Wraps the list of {@link StreamlineEvent} produced as a result
 * of applying the action and the output stream where this
 * has to be sent.
 */
public class Result {
    public final String stream;
    public final List<StreamlineEvent> events;
    /**
     * Create a new Result
     *
     * @param stream the stream where the result has to be sent
     * @param events the list of events in the result
     */
    public Result(String stream, List<StreamlineEvent> events) {
        this.stream = stream;
        this.events = events;
    }

    @Override
    public String toString() {
        return "Result{" +
                "stream='" + stream + '\'' +
                ", events=" + events +
                '}';
    }
}
