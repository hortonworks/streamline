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

import com.hortonworks.streamline.common.exception.ConfigException;

import java.util.Map;

/**
 * Base implementation that can be extended by simple implementations.
 */
public abstract class SimpleCustomProcessorRuntime implements CustomProcessorRuntime {
    @Override
    public void validateConfig(Map<String, Object> config) throws ConfigException {

    }

    @Override
    public void initialize(Map<String, Object> config) {

    }

    @Override
    public void cleanup() {

    }
}
