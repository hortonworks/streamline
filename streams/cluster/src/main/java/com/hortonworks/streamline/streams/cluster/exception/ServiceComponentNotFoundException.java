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
package com.hortonworks.streamline.streams.cluster.exception;

public class ServiceComponentNotFoundException extends EntityNotFoundException {
    public ServiceComponentNotFoundException(String message) {
        super(message);
    }

    public ServiceComponentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceComponentNotFoundException(Throwable cause) {
        super(cause);
    }

    public ServiceComponentNotFoundException(Long clusterId, String serviceName, String componentName) {
        this(String.format("Component [%s] not found for service [%s] in cluster with id [%d]", componentName, serviceName, clusterId));
    }
}
