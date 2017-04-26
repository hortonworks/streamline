package com.hortonworks.streamline.common.function;

public interface FunctionException<I, O, E extends Exception> {
    O apply(I i) throws E;
}
