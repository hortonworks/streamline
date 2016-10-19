package org.apache.streamline.registries.tag.service;

public class TagNotEmptyException extends RuntimeException {
    public TagNotEmptyException(String message) {
        super(message);
    }
}
