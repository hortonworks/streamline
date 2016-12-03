package org.apache.streamline.streams.service.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public abstract class StreamServiceException extends RuntimeException {
  protected Response response;

  protected StreamServiceException(Response.Status status, String msg) {
    super(msg);
    response = Response.status(status)
        .entity(convertToErrorResponseMessage(msg))
        .type(MediaType.APPLICATION_JSON_TYPE)
        .build();
  }

  protected StreamServiceException(Response.Status status, String msg, Throwable cause) {
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
