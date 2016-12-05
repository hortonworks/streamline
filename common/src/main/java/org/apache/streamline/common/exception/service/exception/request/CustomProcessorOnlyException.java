package org.apache.streamline.common.exception.service.exception.request;

import org.apache.streamline.common.exception.service.exception.WebServiceException;

import javax.ws.rs.core.Response;

public class CustomProcessorOnlyException extends WebServiceException {
  private static final String MESSAGE = "Custom endpoint supported only for processors.";

  public CustomProcessorOnlyException() {
    super(Response.Status.BAD_REQUEST, MESSAGE);
  }

  public CustomProcessorOnlyException(Throwable cause) {
    super(Response.Status.BAD_REQUEST, MESSAGE, cause);
  }
}
