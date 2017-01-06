package com.hortonworks.streamline.streams.rule;

/**
 * <p>
 * Interface for user defined aggregate functions (UDAF) of single argument.
 * E.g. min(x), max(y)
 * <p>
 * Performing the aggregation operation over a group of values
 * should produce a result equivalent to:
 * <pre>
 *   A aggregate = udafObj.init();
 *   // for each entity in the group
 *   for (...) {
 *     aggregate = udafObj.add(aggregate, value);
 *   }
 *   R result = udafObj.result(aggregate);
 * </pre>
 *
 * @param <A> the aggregate type
 * @param <V> the value type
 * @param <R> the result type
 */
public interface UDAF<A, V, R> {
    /**
     * Initial value for the aggregator.
     *
     * @return the initial value
     */
    A init();

    /**
     * Return a new aggregate by applying the current value with the accumulated value.
     *
     * @param aggregate the current aggregate
     * @param val       the current value
     * @return the new aggregate
     */
    A add(A aggregate, V val);

    /**
     * Returns the result of the aggregate.
     *
     * @param aggregate the current aggregate
     * @return the result
     */
    R result(A aggregate);
}
