package com.hortonworks.iotas.streams.udaf;

import org.junit.Assert;
import org.junit.Test;

public class StddevpTest {
    @Test
    public void testResult() throws Exception {
        Stddevp stddevp = new Stddevp();
        Stddev stddev = new Stddev();
        Variancep variancep = new Variancep();
        Variance variance = new Variance();
        StddevOnline stddevpAgg = stddevp.init();
        StddevOnline stddevAgg = stddevp.init();
        StddevOnline variancepAgg = stddevp.init();
        StddevOnline varianceAgg = stddevp.init();
        double arr[] = {1, 2, 2, 3, 3, 4, 5};
        double sum = 0;
        for (double i : arr) {
            stddevpAgg = stddevp.add(stddevpAgg, i);
            stddevAgg = stddev.add(stddevAgg, i);
            variancepAgg = stddev.add(variancepAgg, i);
            varianceAgg = stddev.add(varianceAgg, i);
            sum += i;
        }
        double mean = sum / arr.length;
        double sqsum = 0.0;
        for (double i : arr) {
            sqsum += (mean - i) * (mean - i);
        }
        Assert.assertEquals(Math.sqrt(sqsum / arr.length), stddevp.result(stddevpAgg), .0001);
        Assert.assertEquals(Math.sqrt(sqsum / (arr.length - 1)), stddev.result(stddevAgg), .0001);
        Assert.assertEquals(sqsum / arr.length, variancep.result(variancepAgg), .0001);
        Assert.assertEquals(sqsum / (arr.length - 1), variance.result(varianceAgg), .0001);
    }

}