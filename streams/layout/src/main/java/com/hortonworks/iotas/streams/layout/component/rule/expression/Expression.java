package com.hortonworks.iotas.streams.layout.component.rule.expression;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

/**
 * The base class for any condition expression node.
  */
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="class")
public abstract class Expression implements Serializable {
    public abstract void accept(ExpressionVisitor visitor);
}
