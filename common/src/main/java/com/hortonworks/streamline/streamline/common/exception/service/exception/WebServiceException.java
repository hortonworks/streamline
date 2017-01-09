package org.apache.streamline.common.exception.service.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public abstract class WebServiceException extends RuntimeException {
  protected Response response;

  protected WebServiceException(Response.Status status, String msg) {
    super(msg);
    response = Response.status(status)
        .entity(convertToErrorResponseMessage(msg))
        .type(MediaType.APPLICATION_JSON_TYPE)
        .build();
  }

  protected WebServiceException(Response.Status status, String msg, Throwable cause) {
    super(msg, cause);
    response = Response.status(status)
        .entity(convertToErrorResponseMessage(msg))
        .type(MediaType.APPLICATION_JSON_TYPE)
        .build();
  }

  public Response getResponse() {
    return response;
  }

  private static ErrorResponse convertToErrorResponseMessage(String msg) {
    return new ErrorResponse(msg);
  }

  private static class ErrorResponse {
    /**
     * Response message.
     */
    private String responseMessage;

    public ErrorResponse(String responseMessage) {
      this.responseMessage = responseMessage;
    }

    public String getResponseMessage() {
      return responseMessage;
    }
  }
}
