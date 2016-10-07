package org.apache.streamline.streams.udaf;

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
