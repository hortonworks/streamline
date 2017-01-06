package com.hortonworks.streamline.streams.layout.component.rule.expression;

public class Udf {
    public enum Type {
        FUNCTION, AGGREGATE
    }
    private final String name;
    private final Type type;
    private final String className;

    public Udf(String name, String className, Type type) {
        this.name = name;
        this.className = className;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public String getClassName() {
        return className;
    }

    public boolean isAggregate() {
        return type == Type.AGGREGATE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Udf udf = (Udf) o;

        if (name != null ? !name.equals(udf.name) : udf.name != null) return false;
        if (type != udf.type) return false;
        return className != null ? className.equals(udf.className) : udf.className == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (className != null ? className.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Udf{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", className='" + className + '\'' +
                '}';
    }
}
