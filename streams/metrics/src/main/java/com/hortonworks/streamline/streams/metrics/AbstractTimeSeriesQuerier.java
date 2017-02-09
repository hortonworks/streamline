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
package com.hortonworks.streamline.streams.metrics;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for implementations of TimeSeriesQuerier
 */
public abstract class AbstractTimeSeriesQuerier implements TimeSeriesQuerier {
    protected Map<String, String> parseParameters(String parameters) {
        Map<String, String> parsed = new HashMap<>();
        if (parameters != null && !parameters.trim().isEmpty()) {
            String[] splittedParams = parameters.split(",");
            for (String splittedParam : splittedParams) {
                String[] keyValue = splittedParam.split("=");
                if (keyValue.length < 2) {
                    throw new IllegalArgumentException("parameters contain broken key-value pair: " + splittedParam);
                }

                StringBuilder sb = new StringBuilder();
                for (int idx = 1 ; idx < keyValue.length ; idx++) {
                    if (idx != 1) {
                        sb.append('=');
                    }
                    sb.append(keyValue[idx].trim());
                }

                parsed.put(keyValue[0], sb.toString());
            }
        }
        return parsed;
    }
}
