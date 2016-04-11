package com.hortonworks.iotas.service;

public class TagNotEmptyException extends RuntimeException {
    public TagNotEmptyException(String message) {
        super(message);
    }
}
