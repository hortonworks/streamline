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
package com.hortonworks.streamline.streams.udaf;

import org.junit.Assert;
import org.junit.Test;

public class StddevpTest {
    @Test
    public void testResult() throws Exception {
        Stddevp stddevp = new Stddevp();
        Stddev stddev = new Stddev();
        Variancep variancep = new Variancep();
        Variance variance = new Variance();
        Mean meanObj = new Mean();
        StddevOnline stddevpAgg = stddevp.init();
        StddevOnline stddevAgg = stddevp.init();
        StddevOnline variancepAgg = stddevp.init();
        StddevOnline varianceAgg = stddevp.init();
        StddevOnline meanAgg = stddevp.init();
        double arr[] = {1, 2, 2, 3, 3, 4, 5};
        double sum = 0;
        for (double i : arr) {
            stddevpAgg = stddevp.add(stddevpAgg, i);
            stddevAgg = stddev.add(stddevAgg, i);
            variancepAgg = stddev.add(variancepAgg, i);
            varianceAgg = stddev.add(varianceAgg, i);
            meanAgg = meanObj.add(meanAgg, i);
            sum += i;
        }
        double mean = sum / arr.length;
        double sqsum = 0.0;
        for (double i : arr) {
            sqsum += (mean - i) * (mean - i);
        }
        Assert.assertEquals(mean, meanObj.result(meanAgg), .0001);
        Assert.assertEquals(Math.sqrt(sqsum / arr.length), stddevp.result(stddevpAgg), .0001);
        Assert.assertEquals(Math.sqrt(sqsum / (arr.length - 1)), stddev.result(stddevAgg), .0001);
        Assert.assertEquals(sqsum / arr.length, variancep.result(variancepAgg), .0001);
        Assert.assertEquals(sqsum / (arr.length - 1), variance.result(varianceAgg), .0001);
    }

}