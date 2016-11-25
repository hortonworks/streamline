package org.apache.streamline.streams.service.exception.request;

import org.apache.streamline.streams.service.exception.StreamServiceException;

import javax.ws.rs.core.Response;

public class EntityAlreadyExistsException extends StreamServiceException {
  private static final String BY_ID_MESSAGE = "Entity with id [%s] already exists.";
  private static final String BY_NAME_MESSAGE = "Entity with name [%s] already exists.";

  private EntityAlreadyExistsException(String message) {
    super(Response.Status.CONFLICT, message);
  }

  private EntityAlreadyExistsException(String message, Throwable cause) {
    super(Response.Status.CONFLICT, message, cause);
  }

  public static EntityAlreadyExistsException byId(String id) {
    return new EntityAlreadyExistsException(buildMessageByID(id));
  }

  public static EntityAlreadyExistsException byId(String id, Throwable cause) {
    return new EntityAlreadyExistsException(buildMessageByID(id), cause);
  }

  public static EntityAlreadyExistsException byName(String name) {
    return new EntityAlreadyExistsException(buildMessageByName(name));
  }

  public static EntityAlreadyExistsException byName(String name, Throwable cause) {
    return new EntityAlreadyExistsException(buildMessageByName(name), cause);
  }

  private static String buildMessageByID(String id) {
    return String.format(BY_ID_MESSAGE, id);
  }

  private static String buildMessageByName(String name) {
    return String.format(BY_NAME_MESSAGE, name);
  }
}
