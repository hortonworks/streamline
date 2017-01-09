package com.hortonworks.streamline.common.exception.service.exception.request;

import com.hortonworks.streamline.common.exception.service.exception.WebServiceException;

import javax.ws.rs.core.Response;

public class ParserSchemaForEntityNotFoundException extends WebServiceException {
  private static final String MESSAGE = "Parser schema not found for entity with id [%s].";

  public ParserSchemaForEntityNotFoundException(String id) {
    super(Response.Status.NOT_FOUND, String.format(MESSAGE, id));
  }

  public ParserSchemaForEntityNotFoundException(String id, Throwable cause) {
    super(Response.Status.NOT_FOUND, String.format(MESSAGE, id), cause);
  }
}
