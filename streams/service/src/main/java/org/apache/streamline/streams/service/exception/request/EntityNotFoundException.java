package org.apache.streamline.streams.service.exception.request;

import org.apache.streamline.streams.service.exception.StreamServiceException;

import javax.ws.rs.core.Response;

public class EntityNotFoundException extends StreamServiceException {
  private static final String BY_ID_MESSAGE = "Entity with id [%s] not found.";
  private static final String BY_NAME_MESSAGE = "Entity with name [%s] not found.";
  private static final String BY_FILTER_MESSAGE = "Entity not found for query params [%s].";
  private static final String BY_VERSION_MESSAGE = "Entity with id [%s] and version [%s] not found.";

  private EntityNotFoundException(String message) {
    super(Response.Status.NOT_FOUND, message);
  }

  private EntityNotFoundException(String message, Throwable cause) {
    super(Response.Status.NOT_FOUND, message, cause);
  }

  public static EntityNotFoundException byId(String id) {
    return new EntityNotFoundException(buildMessageByID(id));
  }

  public static EntityNotFoundException byId(String id, Throwable cause) {
    return new EntityNotFoundException(buildMessageByID(id), cause);
  }

  public static EntityNotFoundException byName(String name) {
    return new EntityNotFoundException(buildMessageByName(name));
  }

  public static EntityNotFoundException byName(String name, Throwable cause) {
    return new EntityNotFoundException(buildMessageByName(name), cause);
  }

  public static EntityNotFoundException byFilter(String parameter) {
    return new EntityNotFoundException(buildMessageByFilter(parameter));
  }

  public static EntityNotFoundException byFilter(String parameter, Throwable cause) {
    return new EntityNotFoundException(buildMessageByFilter(parameter), cause);
  }

  public static EntityNotFoundException byVersion(String id, String version) {
    return new EntityNotFoundException(buildMessageByVersion(id, version));
  }

  public static EntityNotFoundException byVersion(String id, String version, Throwable cause) {
    return new EntityNotFoundException(buildMessageByVersion(id, version), cause);
  }

  private static String buildMessageByID(String id) {
    return String.format(BY_ID_MESSAGE, id);
  }

  private static String buildMessageByName(String name) {
    return String.format(BY_NAME_MESSAGE, name);
  }

  private static String buildMessageByFilter(String parameter) {
    return String.format(BY_FILTER_MESSAGE, parameter);
  }

  private static String buildMessageByVersion(String id, String version) {
    return String.format(BY_VERSION_MESSAGE, id, version);
  }
}
