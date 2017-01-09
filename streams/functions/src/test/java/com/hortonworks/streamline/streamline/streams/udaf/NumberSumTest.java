package com.hortonworks.streamline.streams.udaf;

import com.hortonworks.streamline.streams.udaf.NumberSum;
import org.junit.Assert;
import org.junit.Test;

public class NumberSumTest {
    @Test
    public void testSum() {
        Byte[] byteArr = new Byte[]{1, 2, 3, 4};
        Short[] shortArr = new Short[]{1, 2, 3, 4};
        Integer[] intArr = new Integer[]{1, 2, 3, 4};
        Long[] longArr = new Long[]{1L, 2L, 3L, 4L};
        Float[] floatArr = new Float[]{1.0f, 2.0f, 3.0f, 4.0f};
        Double[] doubleArr = new Double[]{1.0, 2.0, 3.0, 4.0};
        test(floatArr, Float.class);
        test(doubleArr, Double.class);
        test(byteArr, Byte.class);
        test(shortArr, Short.class);
        test(intArr, Integer.class);
        test(longArr, Long.class);
    }

    void test(Number[] arr, Class<? extends Number> clazz) {
        NumberSum sum = new NumberSum();
        Number agg = sum.init();
        for (Number o : arr) {
            agg = sum.add(agg, o);
        }
        Assert.assertEquals(clazz, agg.getClass());
        Assert.assertEquals(10, agg.intValue());
    }
}