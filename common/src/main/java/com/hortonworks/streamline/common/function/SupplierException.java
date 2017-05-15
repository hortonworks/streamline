package com.hortonworks.streamline.common.function;

public interface SupplierException<T, E extends Exception> {
    T get() throws E;
}
