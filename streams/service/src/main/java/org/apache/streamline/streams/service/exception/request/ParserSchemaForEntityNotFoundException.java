package org.apache.streamline.streams.service.exception.request;

import org.apache.streamline.streams.service.exception.StreamServiceException;

import javax.ws.rs.core.Response;

public class ParserSchemaForEntityNotFoundException extends StreamServiceException {
  private static final String MESSAGE = "Parser schema not found for entity with id [%s].";

  public ParserSchemaForEntityNotFoundException(String id) {
    super(Response.Status.NOT_FOUND, String.format(MESSAGE, id));
  }

  public ParserSchemaForEntityNotFoundException(String id, Throwable cause) {
    super(Response.Status.NOT_FOUND, String.format(MESSAGE, id), cause);
  }
}
