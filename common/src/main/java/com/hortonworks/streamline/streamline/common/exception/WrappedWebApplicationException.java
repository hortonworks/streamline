package org.apache.streamline.common.exception;

import org.apache.streamline.common.exception.service.exception.WebServiceException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class WrappedWebApplicationException extends WebServiceException {
    protected WrappedWebApplicationException(Response.Status status, String msg, Throwable cause) {
        super(status, msg, cause);
    }

    public static WrappedWebApplicationException of(WebApplicationException ex) {
        return of(ex, ex.getMessage());
    }

    public static WrappedWebApplicationException of(WebApplicationException ex, String message) {
        Response.Status status = Response.Status.fromStatusCode(ex.getResponse().getStatus());
        return new WrappedWebApplicationException(status, message, ex);
    }
}
