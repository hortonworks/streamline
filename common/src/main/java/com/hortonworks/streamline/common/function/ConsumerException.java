package com.hortonworks.streamline.common.function;

public interface ConsumerException<T, E extends Exception> {
    void accept(T t) throws E;
}
