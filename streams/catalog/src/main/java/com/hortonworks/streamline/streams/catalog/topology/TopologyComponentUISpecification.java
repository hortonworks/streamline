package org.apache.streamline.streams.catalog.topology;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.streams.layout.exception.ComponentConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class represents the java equivalent of json specification for a component(source/processor/sink) to be added to streams analytics platform. By doing
 * so users can add new components to streams builder ui without waiting on streams platform to support more components corresponding to external systems.
 * Once a user registers a component using this spec, UI will use it in streams builder to elicit input from the user related to that component. This spec
 * also supports use cases where simple key value fields for a component did not suffice. For example, if a user wants to be able to pick one of the two
 * implementations for an interface like RotationPolicy for Hdfs sink, then they will be able to do so. An example json for this spec is at
 * http://www.jsoneditoronline.org/?id=801391b34161aef0cde43860504712a5  It supports basic types in json like boolean, number, string. It also supports json
 * that UI will use to recurse. At top level the spec is just a list of {@link TopologyComponentUISpecification.UIField}
 * For all the basic types (string, boolean and number), the fields property is null. For object type, fields is mandatory to specify the fields in the object
 * in a recursive way. The options property is mandatory for all types that are enum. The default value applies to all basic types and enum types.
 * Below are the different types and their explanations
 *
 * type             | comment
 * ------------------------------------------------------------------------------------------------------------------------------------------------------------
 * string           | if the component needs to capture a simple string field input from user. For example, a kafka topic
 * enumstring       | if the component needs to force user to pick one of the predefined values. For example, rotationIntervalUnit in TimedRotationPolicy
 * array.string     | array of string type described above
 * array.enumstring | array of enumstring above, where all the elements in the array come from the predefined options
 * number           | if the component needs to capture a simple number field input from user. For example, fetchSize in kafka spout
 * array.number     | array of number described above
 * boolean          | if the component needs to capture a simple boolean field input from user. For example, writeToWAL in hbase
 * array.boolean    | array of boolean type described above
 * object           | if the component wants to capture values to create a top level object in a recursive fashion. For example, SpoutConfig in kafka
 * enumobject       | if the component wants to capture values for one of the implementations of an intercace. For example, rotationPolicy in hdfs
 * array.object     | array of object type described above, For example zkServers. fields will describe ZkServer object and ui will let user add more of those
 * array.enumobject | array of objects where each element in array is one of the options provided for this type
 */
public class TopologyComponentUISpecification {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyComponentUISpecification.class);
    private static final String NOT_APPLICABLE_PROPERTY = "%s property for %s of type %s is not applicable";
    private static final String DEFAULT_VALUE_TYPE = "expected type for defaultValue for field %s of type %s is %s actual type %s";
    private static final String PROPERTY_REQUIRED = "property %s required for field %s of type %s";
    public enum UIFieldType {
        STRING("string"), ENUMSTRING("enumstring"), ARRAYSTRING("array.string"), ARRAYENUMSTRING("array.enumstring"),
        NUMBER("number"), ARRAYNUMBER("array.number"), BOOLEAN("boolean"), ARRAYBOOLEAN("array.boolean"),
        OBJECT("object"), ENUMOBJECT("enumobject"), ARRAYOBJECT("array.object"), ARRAYENUMOBJECT("array.enumobject");

        private String uiFieldTypeText;

        UIFieldType (String uiFieldTypeText) {
            this.uiFieldTypeText = uiFieldTypeText;
        }

        @JsonValue
        public String getUiFieldTypeText () {
            return this.uiFieldTypeText;
        }

        @Override
        public String toString() {
            return "UIFieldType{" +
                    "uiFieldTypeText='" + uiFieldTypeText + '\'' +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UIField {
        private static final String UI_NAME = "uiName";
        private static final String FIELD_NAME = "fieldName";
        private static final String IS_USER_INPUT = "isUserInput";
        private static final String TOOLTIP = "tooltip";
        private static final String IS_OPTIONAL = "isOptional";
        private static final String TYPE = "type";
        private static final String DEFAULT_VALUE = "defaultValue";
        private static final String FIELDS = "fields";
        private static final String OPTIONS = "options";
        private static final String HINT = "hint";
        private String uiName;
        private String fieldName;
        private boolean isUserInput = true;
        private String tooltip;
        private boolean isOptional;
        private UIFieldType type;
        private Object defaultValue;
        private List<UIField> fields;
        private List options;
        // A field to hint UI any special handling. For example, password field, email field, schema field, etc
        private String hint;

        public UIField () {}

        public UIField (UIField uiField) {
            this.defaultValue = uiField.defaultValue;
            this.fieldName = uiField.fieldName;
            this.fields = uiField.fields;
            this.isOptional = uiField.isOptional;
            this.isUserInput = uiField.isUserInput;
            this.options = uiField.options;
            this.tooltip = uiField.tooltip;
            this.type = uiField.type;
            this.uiName = uiField.uiName;
            this.hint = uiField.hint;
        }
        public String getUiName() {
            return uiName;
        }

        public void setUiName(String uiName) {
            this.uiName = uiName;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public boolean getIsUserInput() {
            return isUserInput;
        }

        public void setIsUserInput(boolean isUserInput) {
            this.isUserInput = isUserInput;
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

        public UIFieldType getType() {
            return type;
        }

        public void setType(UIFieldType type) {
            this.type = type;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

        public List<UIField> getFields() {
            return fields;
        }

        public void setFields(List<UIField> fields) {
            this.fields = fields;
        }

        public List getOptions() {
            return options;
        }

        public void setOptions(List options) {
            this.options = options;
        }

        public String getHint() {
            return hint;
        }

        public void setHint(String hint) {
            this.hint = hint;
        }

        @Override
        public String toString() {
            return "UIField{" +
                    "uiName='" + uiName + '\'' +
                    ", fieldName='" + fieldName + '\'' +
                    ", isUserInput=" + isUserInput +
                    ", tooltip='" + tooltip + '\'' +
                    ", isOptional=" + isOptional +
                    ", type=" + type +
                    ", defaultValue=" + defaultValue +
                    ", fields=" + fields +
                    ", options=" + options +
                    ", hint=" + hint +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UIField uiField = (UIField) o;

            if (isUserInput != uiField.isUserInput) return false;
            if (isOptional != uiField.isOptional) return false;
            if (uiName != null ? !uiName.equals(uiField.uiName) : uiField.uiName != null) return false;
            if (fieldName != null ? !fieldName.equals(uiField.fieldName) : uiField.fieldName != null) return false;
            if (tooltip != null ? !tooltip.equals(uiField.tooltip) : uiField.tooltip != null) return false;
            if (type != uiField.type) return false;
            if (defaultValue != null ? !defaultValue.equals(uiField.defaultValue) : uiField.defaultValue != null) return false;
            if (fields != null ? !fields.equals(uiField.fields) : uiField.fields != null) return false;
            if (options != null ? !options.equals(uiField.options) : uiField.options != null) return false;
            return !(hint != null ? !hint.equals(uiField.hint) : uiField.hint != null);

        }

        @Override
        public int hashCode() {
            int result = uiName != null ? uiName.hashCode() : 0;
            result = 31 * result + (fieldName != null ? fieldName.hashCode() : 0);
            result = 31 * result + (isUserInput ? 1 : 0);
            result = 31 * result + (tooltip != null ? tooltip.hashCode() : 0);
            result = 31 * result + (isOptional ? 1 : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
            result = 31 * result + (fields != null ? fields.hashCode() : 0);
            result = 31 * result + (options != null ? options.hashCode() : 0);
            result = 31 * result + (hint != null ? hint.hashCode() : 0);
            return result;
        }

        public void validate () throws ComponentConfigException {
            validateCommon();
            validateBooleanField();
            validateStringField();
            validateNumberField();
            validateObjectField();
        }

        private void validateCommon () throws ComponentConfigException {
            String formatString = "{} for field {} cannot be null or empty.";
            String message;
            if (fieldName == null || fieldName.isEmpty()) {
                message = String.format(formatString, fieldName, this);
                logAndThrowException(message);
            }
            if (uiName == null || uiName.isEmpty()) {
                message = String.format(formatString, uiName, this);
                logAndThrowException(message);
            }
            if (tooltip == null || tooltip.isEmpty()) {
                message = String.format(formatString, tooltip, this);
                logAndThrowException(message);
            }
        }

        private void validateBooleanField () throws ComponentConfigException {
            UIFieldType[] booleanTypes = {UIFieldType.BOOLEAN, UIFieldType.ARRAYBOOLEAN};
            if (!Arrays.asList(booleanTypes).contains(type)) {
                return;
            }
            if (fields != null) {
                logAndThrowException(String.format(NOT_APPLICABLE_PROPERTY, FIELDS, fieldName, type));
            }
            if (options != null) {
                logAndThrowException(String.format(NOT_APPLICABLE_PROPERTY, OPTIONS, fieldName, type));
            }
            if (defaultValue != null) {
                // defaultValue for array.boolean not applicable.
                if (UIFieldType.ARRAYBOOLEAN.equals(type)) {
                    logAndThrowException(String.format(NOT_APPLICABLE_PROPERTY, DEFAULT_VALUE, fieldName, type));
                }
                if (!(defaultValue instanceof Boolean)) {
                    logAndThrowException(String.format(DEFAULT_VALUE_TYPE, fieldName, type, Boolean.class.getCanonicalName(), defaultValue.getClass().getCanonicalName()));
                }
            }
        }

        private void validateStringField () throws ComponentConfigException {
            UIFieldType[] stringTypes = {UIFieldType.STRING, UIFieldType.ENUMSTRING, UIFieldType.ARRAYSTRING, UIFieldType.ARRAYENUMSTRING};
            if (!Arrays.asList(stringTypes).contains(this.type)) {
                return;
            }
            if (fields != null) {
                logAndThrowException(String.format(NOT_APPLICABLE_PROPERTY, FIELDS, fieldName, type));
            }
            if (type.equals(UIFieldType.ENUMSTRING) || type.equals(UIFieldType.ARRAYENUMSTRING)) {
                // options are mandatory for enum types
                if (options == null) {
                    logAndThrowException(String.format(PROPERTY_REQUIRED, OPTIONS, fieldName, type));
                }
                for (Object option: options) {
                    if (!(option instanceof String)) {
                        logAndThrowException(String.format("option value for field %s should be a String. Actual received: %s", fieldName, option.getClass()
                                .getCanonicalName()));
                    }
                }
            } else if (options != null) {
                logAndThrowException(String.format(NOT_APPLICABLE_PROPERTY, OPTIONS, fieldName, type));
            }
            if (defaultValue != null) {
                if (type.equals(UIFieldType.ARRAYSTRING) || type.equals(UIFieldType.ARRAYENUMSTRING)) {
                    //default value not applicable for array fields
                    logAndThrowException(String.format(NOT_APPLICABLE_PROPERTY, DEFAULT_VALUE, fieldName, type));
                } else {
                    if (!(defaultValue instanceof String)) {
                        logAndThrowException(String.format(DEFAULT_VALUE_TYPE, fieldName, type, String.class.getCanonicalName(), defaultValue.getClass()
                                .getCanonicalName()));
                    }
                    if (UIFieldType.ENUMSTRING.equals(type)) {
                        //defaultValue has to be one of the options
                        boolean matchFound = false;
                        for (Object option : options) {
                            if (defaultValue.equals(option)) {
                                matchFound = true;
                                break;
                            }
                        }
                        if (!matchFound) {
                            logAndThrowException(String.format("defaultValue %s for field %s of type %s is not one of the options provided", defaultValue,
                                    fieldName, type));
                        }
                    }
                }
            }
        }

        private void validateNumberField () throws ComponentConfigException {
            UIFieldType[] numberTypes = {UIFieldType.NUMBER, UIFieldType.ARRAYNUMBER};
            if (!Arrays.asList(numberTypes).contains(this.type)) {
                return;
            }
            if (fields != null) {
                logAndThrowException(String.format(NOT_APPLICABLE_PROPERTY, FIELDS, fieldName, type));
            }
            if (options != null) {
                logAndThrowException(String.format(NOT_APPLICABLE_PROPERTY, OPTIONS, fieldName, type));
            }
            if (defaultValue != null) {
                //defaultValue not supported for array types
                if (type.equals(UIFieldType.ARRAYNUMBER)) {
                    logAndThrowException(String.format(NOT_APPLICABLE_PROPERTY, DEFAULT_VALUE, fieldName, type));
                }
                //jackson deserializes a json number in to one of the below depending on the size and if its not an integer. Hence this validation
                if (!((defaultValue instanceof Integer) || (defaultValue instanceof Long) || (defaultValue instanceof Double))) {
                    logAndThrowException(String.format(DEFAULT_VALUE_TYPE, fieldName, type, " number ", defaultValue.getClass().getCanonicalName()));
                }
            }
        }

        private void validateObjectField () throws ComponentConfigException {
            UIFieldType[] objectTypes = {UIFieldType.OBJECT, UIFieldType.ENUMOBJECT, UIFieldType.ARRAYOBJECT, UIFieldType.ARRAYENUMOBJECT};
            if (!Arrays.asList(objectTypes).contains(this.type)) {
                return;
            }
            if (type.equals(UIFieldType.OBJECT) || type.equals(UIFieldType.ARRAYOBJECT)) {
                //defaultValue for object types not applicable since its supported at field level already
                if (defaultValue != null) {
                    logAndThrowException(String.format(NOT_APPLICABLE_PROPERTY, DEFAULT_VALUE, fieldName, type));
                }
                //options only applicable to enum types
                if (options != null) {
                    logAndThrowException(String.format(NOT_APPLICABLE_PROPERTY, OPTIONS, fieldName, type));
                }
                //fields are mandatory for object type to recurse
                if (fields == null || fields.isEmpty()) {
                    logAndThrowException(String.format(PROPERTY_REQUIRED, FIELDS, fieldName, type));
                }
                Set<String> fieldNames = new HashSet<>();
                for (UIField uiField: fields) {
                    //fieldNames at a level need to be unique
                    if (fieldNames.contains(uiField.fieldName)) {
                        logAndThrowException("fieldName " + uiField.fieldName + " is repeated. Expected to be unique.");
                    }
                    fieldNames.add(uiField.fieldName);
                    // recursively validate a field of an object. Could be a basic type like string, number, boolean or an object again
                    uiField.validate();
                }
            } else {
                //for enum objects, fields are not applicable
                if (fields != null) {
                    logAndThrowException(String.format(NOT_APPLICABLE_PROPERTY, FIELDS, fieldName, type));
                }
                //options are mandatory for enum objects to tell UI how to display fields for a particular option. Each option has to be an object field here
                if (options == null || options.isEmpty()) {
                    logAndThrowException(String.format(PROPERTY_REQUIRED, OPTIONS, fieldName, type));
                }
                Set<String> optionKeys = new HashSet<>();
                ObjectMapper objectMapper = new ObjectMapper();
                for (Object option: options) {
                    try {
                        //deserialize each option in to a UIField and make sure it is of type object
                        UIField optionObject = objectMapper.readValue(objectMapper.writeValueAsString(option), UIField.class);
                        optionKeys.add(optionObject.fieldName);
                        if (!UIFieldType.OBJECT.equals(optionObject.type)) {
                            logAndThrowException(String.format("option %s for field %s of type %s is expected to be of type %s Actual: %s", optionObject,
                                    fieldName, type, UIFieldType.OBJECT, optionObject.type));
                        }
                        //recursively validate each option which is again a UIField of type object
                        optionObject.validate();
                    } catch (IOException e) {
                        logAndThrowException(String.format("Error while parsing option object %s for field %s of type %s ", option, fieldName, type));
                    }
                }
                if (defaultValue != null) {
                    //defaultValue not supported for array types and has to be one of the options for enum
                    if (type.equals(UIFieldType.ARRAYENUMOBJECT)) {
                        logAndThrowException(String.format(NOT_APPLICABLE_PROPERTY, DEFAULT_VALUE, fieldName, type));
                    } else if (!optionKeys.contains(defaultValue)) {
                        logAndThrowException(String.format("defaultValue %s for field %s of type %s is not one of the options provided", defaultValue,
                                fieldName, type));
                    }
                }
            }
        }

    }

    private List<UIField> fields;

    public List<UIField> getFields() {
        return fields;
    }

    public void setFields(List<UIField> fields) {
        this.fields = fields;
    }

    @Override
    public String toString() {
        return "TopologyComponentUISpecification{" +
                "fields=" + fields +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TopologyComponentUISpecification that = (TopologyComponentUISpecification) o;

        return !(fields != null ? !fields.equals(that.fields) : that.fields != null);

    }

    @Override
    public int hashCode() {
        return fields != null ? fields.hashCode() : 0;
    }

    public void validate () throws ComponentConfigException {
        Set<String> fieldNames = new HashSet<>();
        for (UIField uiField: fields) {
            if (fieldNames.contains(uiField.fieldName)) {
                logAndThrowException("fieldName " + uiField.fieldName + " is repeated. Expected to be unique.");
            }
            fieldNames.add(uiField.fieldName);
            uiField.validate();
        }
    }

    private static void logAndThrowException (String message) throws ComponentConfigException {
        LOG.debug(message);
        throw new ComponentConfigException(message);
    }
}
