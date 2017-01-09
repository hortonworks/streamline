package org.apache.streamline.streams.notification.service;

import java.util.NoSuchElementException;

/**
 * A notifier could not be found for the given param.
 */
public class NoSuchNotifierException extends NoSuchElementException {
    public NoSuchNotifierException(String msg) {
        super(msg);
    }
}
