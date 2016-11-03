package org.apache.streamline.streams.service.exception.server;

import org.apache.streamline.streams.service.exception.StreamServiceException;

import javax.ws.rs.core.Response;

public class UnhandledServerException extends StreamServiceException {
  private static final String MESSAGE = "An exception with message [%s] was thrown while processing request.";

  public UnhandledServerException(String exceptionMessage) {
    super(Response.Status.INTERNAL_SERVER_ERROR, String.format(MESSAGE, exceptionMessage));
  }

  public UnhandledServerException(String exceptionMessage, Throwable cause) {
    super(Response.Status.INTERNAL_SERVER_ERROR, String.format(MESSAGE, exceptionMessage), cause);
  }
}
