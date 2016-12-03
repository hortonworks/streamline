package org.apache.streamline.streams.service.exception.request;

import org.apache.streamline.streams.service.exception.StreamServiceException;

import javax.ws.rs.core.Response;

public class CustomProcessorOnlyException extends StreamServiceException {
  private static final String MESSAGE = "Custom endpoint supported only for processors.";

  public CustomProcessorOnlyException() {
    super(Response.Status.BAD_REQUEST, MESSAGE);
  }

  public CustomProcessorOnlyException(Throwable cause) {
    super(Response.Status.BAD_REQUEST, MESSAGE, cause);
  }
}
