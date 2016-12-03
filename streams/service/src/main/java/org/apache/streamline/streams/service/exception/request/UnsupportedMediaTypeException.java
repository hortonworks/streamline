package org.apache.streamline.streams.service.exception.request;

import org.apache.streamline.streams.service.exception.StreamServiceException;

import javax.ws.rs.core.Response;

public class UnsupportedMediaTypeException extends StreamServiceException {
  private static final String MESSAGE = "Unsupported Media Type [%s]";

  public UnsupportedMediaTypeException(String mediaType) {
    super(Response.Status.UNSUPPORTED_MEDIA_TYPE, String.format(MESSAGE, mediaType));
  }

  public UnsupportedMediaTypeException(String mediaType, Throwable cause) {
    super(Response.Status.UNSUPPORTED_MEDIA_TYPE, String.format(MESSAGE, mediaType));
  }
}
