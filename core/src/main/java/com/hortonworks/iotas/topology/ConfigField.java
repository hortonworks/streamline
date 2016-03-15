package com.hortonworks.iotas.topology;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Class representing a config field for UI to use in topology editor to create correct json values
 */
public class ConfigField {

    private String name;
    private String tooltip;
    private boolean isOptional;
    private boolean isUserInput;
    private Object defaultValue;
    private Type type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    public boolean getIsOptional() {
        return isOptional;
    }

    public void setIsOptional(boolean isOptional) {
        this.isOptional = isOptional;
    }

    public boolean getIsUserInput() {
        return isUserInput;
    }

    public void setIsUserInput(boolean isUserInput) {
        this.isUserInput = isUserInput;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigField that = (ConfigField) o;

        if (isOptional != that.isOptional) return false;
        if (isUserInput != that.isUserInput) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (tooltip != null ? !tooltip.equals(that.tooltip) : that.tooltip != null) return false;
        if (defaultValue != null ? !defaultValue.equals(that.defaultValue) : that.defaultValue != null) return false;
        return type == that.type;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (tooltip != null ? tooltip.hashCode() : 0);
        result = 31 * result + (isOptional ? 1 : 0);
        result = 31 * result + (isUserInput ? 1 : 0);
        result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ConfigField{" +
                "name='" + name + '\'' +
                ", tooltip='" + tooltip + '\'' +
                ", isOptional=" + isOptional +
                ", isUserInput=" + isUserInput +
                ", defaultValue=" + defaultValue +
                ", type=" + type +
                '}';
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public enum Type {
        BOOLEAN ("boolean"),
        STRING ("string"),
        NUMBER ("number"),
        OBJECT ("object"),
        ARRAYBOOLEAN ("array.boolean"),
        ARRAYSTRING ("array.string"),
        ARRAYNUMBER("array.number"),
        ARRAYOBJECT("array.object");

        private String name;

        private Type (String name) {
            this.name = name;
        }

        @JsonValue
        public String getName () {
            return this.name;
        }

    }

}
