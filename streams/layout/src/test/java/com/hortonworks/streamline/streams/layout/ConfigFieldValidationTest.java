/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.streams.layout;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class ConfigFieldValidationTest {
    @Test
    public void testBooleanField () {
        Assert.assertTrue(ConfigFieldValidation.isBoolean(true));
        Assert.assertFalse(ConfigFieldValidation.isBoolean(null));
        Assert.assertFalse(ConfigFieldValidation.isBoolean(1));
    }

    @Test
    public void testByteField () {
        Assert.assertTrue(ConfigFieldValidation.isByteAndInRange(0, Byte.MIN_VALUE, Byte.MAX_VALUE));
        Assert.assertFalse(ConfigFieldValidation.isByteAndInRange(null, Byte.MIN_VALUE, Byte.MAX_VALUE));
        Assert.assertFalse(ConfigFieldValidation.isByteAndInRange(Byte.MIN_VALUE - 1, Byte.MIN_VALUE, Byte.MAX_VALUE));
        Assert.assertFalse(ConfigFieldValidation.isByteAndInRange(Byte.MAX_VALUE + 1, Byte.MIN_VALUE, Byte.MAX_VALUE));
    }

    @Test
    public void testShortField () {
        Assert.assertTrue(ConfigFieldValidation.isShortAndInRange(0, Short.MIN_VALUE, Short.MAX_VALUE));
        Assert.assertFalse(ConfigFieldValidation.isShortAndInRange(null, Short.MIN_VALUE, Short.MAX_VALUE));
        Assert.assertFalse(ConfigFieldValidation.isShortAndInRange(Short.MIN_VALUE - 1, Short.MIN_VALUE, Short.MAX_VALUE));
        Assert.assertFalse(ConfigFieldValidation.isShortAndInRange(Short.MAX_VALUE + 1, Short.MIN_VALUE, Short.MAX_VALUE));
    }

    @Test
    public void testIntField () {
        Assert.assertTrue(ConfigFieldValidation.isIntAndInRange(0, Integer.MIN_VALUE, Integer.MAX_VALUE));
        Assert.assertFalse(ConfigFieldValidation.isIntAndInRange(null, Integer.MIN_VALUE, Integer.MAX_VALUE));
        Assert.assertFalse(ConfigFieldValidation.isIntAndInRange(Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MAX_VALUE));
        Assert.assertFalse(ConfigFieldValidation.isIntAndInRange(Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE - 1));
    }

    @Test
    public void testLongField () {
        Assert.assertTrue(ConfigFieldValidation.isLongAndInRange(0, Long.MIN_VALUE, Long.MAX_VALUE));
        Assert.assertFalse(ConfigFieldValidation.isLongAndInRange(null, Long.MIN_VALUE, Long.MAX_VALUE));
        Assert.assertFalse(ConfigFieldValidation.isLongAndInRange(Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MAX_VALUE));
        Assert.assertFalse(ConfigFieldValidation.isLongAndInRange(Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE - 1));
    }

    @Test
    public void testDoubleOrFloatField () {
        Assert.assertTrue(ConfigFieldValidation.isFloatOrDouble(0));
        Assert.assertTrue(ConfigFieldValidation.isFloatOrDouble(Integer.MAX_VALUE + 1));
        Assert.assertTrue(ConfigFieldValidation.isFloatOrDouble(0.0d));
        Assert.assertFalse(ConfigFieldValidation.isFloatOrDouble(null));
        Assert.assertFalse(ConfigFieldValidation.isFloatOrDouble("string"));
    }

    @Test
    public void testStringField () {
        Assert.assertTrue(ConfigFieldValidation.isStringAndNotEmpty("string"));
        Assert.assertFalse(ConfigFieldValidation.isStringAndNotEmpty(0));
        Assert.assertFalse(ConfigFieldValidation.isStringAndNotEmpty(null));
        Assert.assertFalse(ConfigFieldValidation.isStringAndNotEmpty(""));
    }

    @Test
    public void testListField () {
        Assert.assertTrue(ConfigFieldValidation.isList(new ArrayList()));
        Assert.assertFalse(ConfigFieldValidation.isList(null));
        Assert.assertFalse(ConfigFieldValidation.isList(0));
        Assert.assertFalse(ConfigFieldValidation.isList("string"));
    }
}
