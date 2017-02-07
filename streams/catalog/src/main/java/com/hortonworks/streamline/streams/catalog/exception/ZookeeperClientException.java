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
package com.hortonworks.streamline.streams.catalog.exception;


/**
 * Wraps Curator Framework exceptions. It is useful to do this because several Curator Framework methods throw the generic
 * {@link Exception}, which makes it impossible to handle more specific exceptions in code that calls these methods.
 */
public class ZookeeperClientException extends Exception {
    public ZookeeperClientException(String message) {
        super(message);
    }

    public ZookeeperClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZookeeperClientException(Throwable cause) {
        super(cause);
    }
}
