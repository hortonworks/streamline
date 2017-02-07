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
package com.hortonworks.streamline.common.exception.service.exception.server;

import com.hortonworks.streamline.common.exception.service.exception.WebServiceException;

import javax.ws.rs.core.Response;

public class UnhandledServerException extends WebServiceException {
  private static final String MESSAGE = "An exception with message [%s] was thrown while processing request.";

  public UnhandledServerException(String exceptionMessage) {
    super(Response.Status.INTERNAL_SERVER_ERROR, String.format(MESSAGE, exceptionMessage));
  }

  public UnhandledServerException(String exceptionMessage, Throwable cause) {
    super(Response.Status.INTERNAL_SERVER_ERROR, String.format(MESSAGE, exceptionMessage), cause);
  }
}
