package com.hortonworks.streamline.common.function;

public interface PredicateException<T, E extends Exception> {
    boolean test(T t) throws E;
}
