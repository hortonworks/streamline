package org.apache.streamline.streams.service;

import org.apache.streamline.common.exception.service.exception.WebServiceException;
import org.apache.streamline.common.exception.service.exception.request.BadRequestException;
import org.apache.streamline.common.exception.service.exception.server.UnhandledServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {
  private static final Logger LOG = LoggerFactory.getLogger(GenericExceptionMapper.class);

  @Override
  public Response toResponse(Throwable ex) {
    if (ex instanceof ProcessingException) {
      return BadRequestException.of().getResponse();
    } else if (ex instanceof WebServiceException) {
      return ((WebServiceException) ex).getResponse();
    }

    logUnhandledException(ex);
    return new UnhandledServerException(ex.getMessage()).getResponse();
  }

  private void logUnhandledException(Throwable ex) {
    String errMessage = String.format("Got exception: [%s] / message [%s]",
        ex.getClass().getSimpleName(), ex.getMessage());
    StackTraceElement elem = findFirstResourceCallFromCallStack(ex.getStackTrace());
    String resourceClassName = null;
    if (elem != null) {
      errMessage += String.format(" / related resource location: [%s.%s](%s:%d)",
          elem.getClassName(), elem.getMethodName(), elem.getFileName(), elem.getLineNumber());
      resourceClassName = elem.getClassName();
    }

    Logger log = getEffectiveLogger(resourceClassName);
    log.error(errMessage, ex);
  }

  private StackTraceElement findFirstResourceCallFromCallStack(StackTraceElement[] stackTrace) {
    for (StackTraceElement stackTraceElement : stackTrace) {
      try {
        Class<?> aClass = Class.forName(stackTraceElement.getClassName());
        Path pathAnnotation = aClass.getAnnotation(Path.class);

        if (pathAnnotation != null) {
          return stackTraceElement;
        }
      } catch (ClassNotFoundException e) {
        // skip
      }
    }

    return null;
  }

  private Logger getEffectiveLogger(String resourceClassName) {
    Logger log = LOG;
    if (resourceClassName != null) {
      log = LoggerFactory.getLogger(resourceClassName);
    }
    return log;
  }

}
