package com.hortonworks.streamline.common.exception.service.exception.request;

import com.hortonworks.streamline.common.exception.service.exception.WebServiceException;

import javax.ws.rs.core.Response;

public class ClusterImportAlreadyInProgressException extends WebServiceException {
  private static final String MESSAGE = "Cluster [%s] is already in progress of import.";

  public ClusterImportAlreadyInProgressException(String id) {
    super(Response.Status.CONFLICT, String.format(MESSAGE, id));
  }

  public ClusterImportAlreadyInProgressException(String id, Throwable cause) {
    super(Response.Status.CONFLICT, String.format(MESSAGE, id), cause);
  }
}
