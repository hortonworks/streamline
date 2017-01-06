package com.hortonworks.streamline.streams.layout.component.rule.expression;

import com.google.common.collect.ImmutableList;

import java.io.Serializable;
import java.util.List;

/**
 * A user defined or built in function expression
 */
public class FunctionExpression extends Expression {
    private Function function;
    private List<Expression> operands;

    /**
     * Built in function (e.g. built in sql functions like UPPER)
     */
    public FunctionExpression(String name, List<? extends Expression> operands) {
        this(name, null, operands, false);
    }

    /**
     * User defined function
     */
    public FunctionExpression(String name, String className, List<? extends Expression> operands) {
        this(name, className, operands, true);
    }

    private FunctionExpression(String name, String className, List<? extends Expression> operands, boolean udf) {
        this.function = new Function(name, className, udf);
        this.operands = ImmutableList.copyOf(operands);
    }

    // for jackson
    protected FunctionExpression() {
    }

    public Function getFunction() {
        return function;
    }

    public void setFunction(Function function) {
        this.function = function;
    }

    public List<Expression> getOperands() {
        return operands;
    }

    public void setOperands(List<? extends Expression> operands) {
        this.operands = ImmutableList.copyOf(operands);
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FunctionExpression that = (FunctionExpression) o;

        if (function != null ? !function.equals(that.function) : that.function != null) return false;
        return operands != null ? operands.equals(that.operands) : that.operands == null;

    }

    @Override
    public int hashCode() {
        int result = function != null ? function.hashCode() : 0;
        result = 31 * result + (operands != null ? operands.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FunctionExpression{" +
                "function=" + function +
                ", operands=" + operands +
                "}";
    }

    public static class Function implements Serializable {
        private String name;
        private String className;
        private boolean udf;

        /**
         * Built in function (e.g. built in sql functions like UPPER)
         */
        public Function(String name) {
            this(name, null, false);
        }

        /**
         * User defined function
         */
        public Function(String name, String className, boolean udf) {
            this.name = name;
            this.className = className;
            this.udf = udf;
        }

        // for jackson
        private Function() {
        }

        public String getName() {
            return name;
        }

        public String getClassName() {
            return className;
        }

        public boolean isUdf() {
            return udf;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Function function = (Function) o;

            if (udf != function.udf) return false;
            if (name != null ? !name.equals(function.name) : function.name != null) return false;
            return className != null ? className.equals(function.className) : function.className == null;

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (className != null ? className.hashCode() : 0);
            result = 31 * result + (udf ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Function{" +
                    "name='" + name + '\'' +
                    ", className='" + className + '\'' +
                    ", udf=" + udf +
                    '}';
        }
    }
}
