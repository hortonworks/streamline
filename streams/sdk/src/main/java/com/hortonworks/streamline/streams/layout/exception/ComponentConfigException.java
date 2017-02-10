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
package com.hortonworks.streamline.streams.layout.exception;

/**
 * Indicates an issue while trying to validate a json for a topology layout.
 */
public class ComponentConfigException extends Exception {

    public ComponentConfigException(String message) {
        super(message);
    }

    public ComponentConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public ComponentConfigException(Throwable cause) {
        super(cause);
    }
}
