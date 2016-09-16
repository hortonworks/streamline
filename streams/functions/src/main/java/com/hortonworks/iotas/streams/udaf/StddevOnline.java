package com.hortonworks.iotas.streams.udaf;

/**
 * Computes online variance and stddev of values using
 * B.P. Welford's algorithm described in Knuth's TAOCP Vol2. p232, 3rd edition.
 */
public class StddevOnline {
    private int n;
    private double mean;
    private double aggregate;

    StddevOnline add(double val) {
        ++n;
        double delta = val - mean;
        mean += delta / n;
        aggregate += (delta * (val - mean));
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
}
