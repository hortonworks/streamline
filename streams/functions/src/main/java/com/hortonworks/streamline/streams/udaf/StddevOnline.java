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

/**
 * Computes online variance and stddev of values using
 * B.P. Welford's algorithm described in Knuth's TAOCP Vol2. p232, 3rd edition.
 */
public class StddevOnline {
    private int n;
    private double mean;
    private double aggregate;

    StddevOnline add(Number val) {
        ++n;
        double delta = val.doubleValue() - mean;
        mean += delta / n;
        aggregate += (delta * (val.doubleValue() - mean));
        return this;
    }

    double stddevp() {
        return Math.sqrt(variancep());
    }

    double variancep() {
        return aggregate / n;
    }

    double stddev() {
        return Math.sqrt(variance());
    }

    double variance() {
        return aggregate / (n - 1);
    }

    double mean() {
        return mean;
    }
}
