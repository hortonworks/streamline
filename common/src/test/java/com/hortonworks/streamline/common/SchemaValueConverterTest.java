package com.hortonworks.streamline.common;

import com.hortonworks.registries.common.Schema;
import com.hortonworks.registries.common.exception.ParserException;
import com.hortonworks.streamline.common.exception.SchemaValidationFailedException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchemaValueConverterTest {
    @Test
    public void convertMap() throws Exception {
        Schema schema = Schema.of(
                Schema.Field.of("a", Schema.Type.BINARY),
                Schema.Field.of("b", Schema.Type.STRING),
                Schema.Field.of("c", Schema.Type.ARRAY),
                Schema.Field.of("d", Schema.Type.LONG),
                Schema.Field.of("e", Schema.Type.DOUBLE));

        Map<String, Object> value = new HashMap<>();
        value.put("a", new byte[] { 0x01, 0x02 });
        value.put("b", "hello");
        value.put("c", Collections.singletonList("hello"));
        value.put("d", 1234);
        value.put("e", 123.456f);

        Map<String, Object> converted = SchemaValueConverter.convertMap(schema, value);
        Assert.assertEquals(value.size(), converted.size());

        Assert.assertTrue(Schema.Type.BINARY.valueOfSameType(converted.get("a")));
        Assert.assertTrue(Arrays.equals(new byte[] { 0x01, 0x02 }, (byte[]) converted.get("a")));

        Assert.assertTrue(Schema.Type.STRING.valueOfSameType(converted.get("b")));
        Assert.assertEquals("hello", converted.get("b"));

        Assert.assertTrue(Schema.Type.ARRAY.valueOfSameType(converted.get("c")));
        Assert.assertEquals(Collections.singletonList("hello"), converted.get("c"));

        Assert.assertTrue(Schema.Type.LONG.valueOfSameType(converted.get("d")));
        Assert.assertEquals(1234L, converted.get("d"));

        Assert.assertTrue(Schema.Type.DOUBLE.valueOfSameType(converted.get("e")));
        Assert.assertEquals(123.456d, (double) converted.get("e"), 0.001);
    }

    @Test
    public void convertMapValueDoesNotHaveOptionalField() throws ParserException {
        Schema schema = Schema.of(
                Schema.Field.of("a", Schema.Type.BINARY),
                Schema.Field.optional("b", Schema.Type.STRING),
                Schema.Field.of("c", Schema.Type.ARRAY));

        Map<String, Object> value = new HashMap<>();
        value.put("a", new byte[] { 0x01, 0x02 });
        value.put("c", Collections.singletonList("hello"));

        Map<String, Object> converted = SchemaValueConverter.convertMap(schema, value);
        Assert.assertEquals(value.size(), converted.size());

        Assert.assertTrue(Schema.Type.BINARY.valueOfSameType(converted.get("a")));
        Assert.assertTrue(Arrays.equals(new byte[] { 0x01, 0x02 }, (byte[]) converted.get("a")));

        Assert.assertTrue(Schema.Type.ARRAY.valueOfSameType(converted.get("c")));
        Assert.assertEquals(Collections.singletonList("hello"), converted.get("c"));

    }

    @Test(expected = SchemaValidationFailedException.class)
    public void convertMapValueDoesNotHaveRequiredField() {
        Schema schema = Schema.of(
                Schema.Field.of("a", Schema.Type.BINARY),
                Schema.Field.of("b", Schema.Type.STRING),
                Schema.Field.of("c", Schema.Type.ARRAY));

        Map<String, Object> value = new HashMap<>();
        value.put("a", new byte[] { 0x01, 0x02 });
        value.put("c", Collections.singletonList("hello"));

        SchemaValueConverter.convertMap(schema, value);
    }

    @Test(expected = SchemaValidationFailedException.class)
    public void convertMapValueHasUndefinedField() {
        Schema schema = Schema.of(
                Schema.Field.of("a", Schema.Type.BINARY),
                Schema.Field.of("b", Schema.Type.ARRAY),
                Schema.Field.of("c", Schema.Type.STRING));

        Map<String, Object> value = new HashMap<>();
        value.put("a", new byte[] { 0x01, 0x02 });
        value.put("b", Collections.singletonList("hello"));
        value.put("c", "hello");
        value.put("d", "world");

        SchemaValueConverter.convertMap(schema, value);
    }

    @Test
    public void convertBooleanToBoolean() throws ParserException {
        Object converted = SchemaValueConverter.convert(Schema.Type.BOOLEAN, true);
        Assert.assertTrue(Schema.Type.BOOLEAN.valueOfSameType(converted));
        Assert.assertEquals(Boolean.TRUE, converted);

        converted = SchemaValueConverter.convert(Schema.Type.BOOLEAN, false);
        Assert.assertTrue(Schema.Type.BOOLEAN.valueOfSameType(converted));
        Assert.assertEquals(Boolean.FALSE, converted);
    }

    @Test
    public void convertStringToBoolean() throws ParserException {
        Object converted = SchemaValueConverter.convert(Schema.Type.BOOLEAN, "true");
        Assert.assertTrue(Schema.Type.BOOLEAN.valueOfSameType(converted));
        Assert.assertEquals(Boolean.TRUE, converted);

        converted = SchemaValueConverter.convert(Schema.Type.BOOLEAN, "False");
        Assert.assertTrue(Schema.Type.BOOLEAN.valueOfSameType(converted));
        Assert.assertEquals(Boolean.FALSE, converted);

        // this follows the conversion rule of Java Boolean: all the non-true is false
        converted = SchemaValueConverter.convert(Schema.Type.BOOLEAN, "hello");
        Assert.assertTrue(Schema.Type.BOOLEAN.valueOfSameType(converted));
        Assert.assertEquals(Boolean.FALSE, converted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertIntegerToBoolean() {
        SchemaValueConverter.convert(Schema.Type.BOOLEAN, 123);
    }

    @Test
    public void convertByteToByte() throws ParserException {
        Object converted = SchemaValueConverter.convert(Schema.Type.BYTE, Byte.MAX_VALUE);
        Assert.assertTrue(Schema.Type.BYTE.valueOfSameType(converted));
        Assert.assertEquals(Byte.MAX_VALUE, converted);

        converted = SchemaValueConverter.convert(Schema.Type.BYTE, Byte.MIN_VALUE);
        Assert.assertTrue(Schema.Type.BYTE.valueOfSameType(converted));
        Assert.assertEquals(Byte.MIN_VALUE, converted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertBiggerThanByteToByte() {
        SchemaValueConverter.convert(Schema.Type.BYTE, Byte.MAX_VALUE + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertLessThanByteToByte() {
        SchemaValueConverter.convert(Schema.Type.BYTE, Byte.MIN_VALUE - 1);
    }

    @Test
    public void convertShortToShort() throws ParserException {
        Object converted = SchemaValueConverter.convert(Schema.Type.SHORT, Short.MAX_VALUE);
        Assert.assertTrue(Schema.Type.SHORT.valueOfSameType(converted));
        Assert.assertEquals(Short.MAX_VALUE, converted);

        converted = SchemaValueConverter.convert(Schema.Type.SHORT, Short.MIN_VALUE);
        Assert.assertTrue(Schema.Type.SHORT.valueOfSameType(converted));
        Assert.assertEquals(Short.MIN_VALUE, converted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertBiggerThanShortToShort() {
        SchemaValueConverter.convert(Schema.Type.SHORT, Short.MAX_VALUE + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertLessThanShortToShort() {
        SchemaValueConverter.convert(Schema.Type.SHORT, Short.MIN_VALUE - 1);
    }

    @Test
    public void convertIntegerToInteger() throws ParserException {
        Object converted = SchemaValueConverter.convert(Schema.Type.INTEGER, Integer.MAX_VALUE);
        Assert.assertTrue(Schema.Type.INTEGER.valueOfSameType(converted));
        Assert.assertEquals(Integer.MAX_VALUE, converted);

        converted = SchemaValueConverter.convert(Schema.Type.INTEGER, Integer.MIN_VALUE);
        Assert.assertTrue(Schema.Type.INTEGER.valueOfSameType(converted));
        Assert.assertEquals(Integer.MIN_VALUE, converted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertBiggerThanIntegerToInteger() {
        SchemaValueConverter.convert(Schema.Type.INTEGER, Integer.MAX_VALUE + 1L);

    }

    @Test(expected = IllegalArgumentException.class)
    public void convertLessThanIntegerToInteger() {
        SchemaValueConverter.convert(Schema.Type.INTEGER, Integer.MIN_VALUE - 1L);
    }

    @Test
    public void convertLongToLong() throws ParserException {
        Object converted = SchemaValueConverter.convert(Schema.Type.LONG, Long.MAX_VALUE);
        Assert.assertTrue(Schema.Type.LONG.valueOfSameType(converted));
        Assert.assertEquals(Long.MAX_VALUE, converted);

        converted = SchemaValueConverter.convert(Schema.Type.LONG, Long.MIN_VALUE);
        Assert.assertTrue(Schema.Type.LONG.valueOfSameType(converted));
        Assert.assertEquals(Long.MIN_VALUE, converted);
    }

    @Test
    public void convertFloatToFloat() throws ParserException {
        Object converted = SchemaValueConverter.convert(Schema.Type.FLOAT, Float.MAX_VALUE);
        Assert.assertTrue(Schema.Type.FLOAT.valueOfSameType(converted));
        Assert.assertEquals(Float.MAX_VALUE, (float) converted, 0.00001);

        converted = SchemaValueConverter.convert(Schema.Type.FLOAT, Float.MIN_VALUE);
        Assert.assertTrue(Schema.Type.FLOAT.valueOfSameType(converted));
        Assert.assertEquals(Float.MIN_VALUE, (float) converted, 0.00001);
    }

    @Test
    public void convertFloatToDouble() throws ParserException {
        Object converted = SchemaValueConverter.convert(Schema.Type.DOUBLE, Float.MAX_VALUE);
        Assert.assertTrue(Schema.Type.DOUBLE.valueOfSameType(converted));
        Assert.assertEquals(Float.MAX_VALUE, (double) converted, 0.00001);

        converted = SchemaValueConverter.convert(Schema.Type.DOUBLE, Float.MIN_VALUE);
        Assert.assertTrue(Schema.Type.DOUBLE.valueOfSameType(converted));
        Assert.assertEquals(Float.MIN_VALUE, (double) converted, 0.00001);
    }

    @Test
    public void convertDoubleToDouble() throws ParserException {
        Object converted = SchemaValueConverter.convert(Schema.Type.DOUBLE, Double.MAX_VALUE);
        Assert.assertTrue(Schema.Type.DOUBLE.valueOfSameType(converted));
        Assert.assertEquals(Double.MAX_VALUE, (double) converted, 0.00001);

        converted = SchemaValueConverter.convert(Schema.Type.DOUBLE, Double.MIN_VALUE);
        Assert.assertTrue(Schema.Type.DOUBLE.valueOfSameType(converted));
        Assert.assertEquals(Double.MIN_VALUE, (double) converted, 0.00001);
    }

    @Test
    public void convertIntegerToDouble() throws ParserException {
        Object converted = SchemaValueConverter.convert(Schema.Type.DOUBLE, Integer.MAX_VALUE);
        Assert.assertTrue(Schema.Type.DOUBLE.valueOfSameType(converted));
        Assert.assertEquals(Integer.MAX_VALUE * 1.0, (double) converted, 0.00001);

        converted = SchemaValueConverter.convert(Schema.Type.DOUBLE, Integer.MIN_VALUE);
        Assert.assertTrue(Schema.Type.DOUBLE.valueOfSameType(converted));
        Assert.assertEquals(Integer.MIN_VALUE * 1.0, (double) converted, 0.00001);
    }

    @Test
    public void convertStringToString() throws ParserException {
        Object converted = SchemaValueConverter.convert(Schema.Type.STRING, "hello");
        Assert.assertTrue(Schema.Type.STRING.valueOfSameType(converted));
        Assert.assertEquals("hello", converted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertNonStringToString() {
        SchemaValueConverter.convert(Schema.Type.STRING, 123);
    }

    @Test
    public void convertByteArrayToBinary() throws ParserException {
        byte[] bytes = {0x01, 0x02};
        Object converted = SchemaValueConverter.convert(Schema.Type.BINARY, bytes);
        Assert.assertTrue(Schema.Type.BINARY.valueOfSameType(converted));
        Assert.assertEquals(bytes, converted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertStringArrayToBinary() {
        String[] bytes = { "0x01", "0x02" };
        SchemaValueConverter.convert(Schema.Type.BINARY, bytes);
    }

    @Test
    public void convertMapToNested() throws ParserException {
        Map<String, String> value = Collections.singletonMap("hello", "world");
        Object converted = SchemaValueConverter.convert(Schema.Type.NESTED, value);
        Assert.assertTrue(Schema.Type.NESTED.valueOfSameType(converted));
        Assert.assertEquals(value, converted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertListToNested() {
        List<String> value = Collections.singletonList("hello");
        SchemaValueConverter.convert(Schema.Type.NESTED, value);
    }

    @Test
    public void convertListToArray() throws ParserException {
        List<String> value = Collections.singletonList("hello");
        Object converted = SchemaValueConverter.convert(Schema.Type.ARRAY, value);
        Assert.assertTrue(Schema.Type.ARRAY.valueOfSameType(converted));
        Assert.assertEquals(value, converted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertMapToArray() {
        Map<String, String> value = Collections.singletonMap("hello", "world");
        SchemaValueConverter.convert(Schema.Type.ARRAY, value);
    }

}