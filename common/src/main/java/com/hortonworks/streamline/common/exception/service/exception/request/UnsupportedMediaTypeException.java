package com.hortonworks.streamline.common.exception.service.exception.request;

import com.hortonworks.streamline.common.exception.service.exception.WebServiceException;

import javax.ws.rs.core.Response;

public class UnsupportedMediaTypeException extends WebServiceException {
  private static final String MESSAGE = "Unsupported Media Type [%s]";

  public UnsupportedMediaTypeException(String mediaType) {
    super(Response.Status.UNSUPPORTED_MEDIA_TYPE, String.format(MESSAGE, mediaType));
  }

  public UnsupportedMediaTypeException(String mediaType, Throwable cause) {
    super(Response.Status.UNSUPPORTED_MEDIA_TYPE, String.format(MESSAGE, mediaType));
  }
}
