package org.apache.streamline.streams.service.exception.request;

import org.apache.streamline.streams.service.exception.StreamServiceException;

import javax.ws.rs.core.Response;

public class ClusterImportAlreadyInProgressException extends StreamServiceException {
  private static final String MESSAGE = "Cluster [%s] is already in progress of import.";

  public ClusterImportAlreadyInProgressException(String id) {
    super(Response.Status.CONFLICT, String.format(MESSAGE, id));
  }

  public ClusterImportAlreadyInProgressException(String id, Throwable cause) {
    super(Response.Status.CONFLICT, String.format(MESSAGE, id), cause);
  }
}
