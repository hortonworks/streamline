package com.hortonworks.iotas.registries.tag.service;

public class TagNotEmptyException extends RuntimeException {
    public TagNotEmptyException(String message) {
        super(message);
    }
}
