package com.hortonworks.streamline.streams.rule;

/**
 * <p>
 * Interface for user defined aggregate functions (UDAF) of two arguments.
 * E.g. topn(5, x)
 * </p>
 * Performing the aggregation operation over a group of values
 * should produce a result equivalent to:
 * <pre>
 *   A aggregate = udafObj.init();
 *   // for each entity in the group
 *   for (...) {
 *     aggregate = udafObj.add(aggregate, value1, value2);
 *   }
 *   R result = udafObj.result(aggregate);
 * </pre>
 * @param <A> the aggregate type
 * @param <V1> the value type of first argument
 * @param <V2> the value type of second argument
 * @param <R> the result type
 */
public interface UDAF2<A, V1, V2, R> {
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
     * @param val1       the current value of the first argument
     * @param val2       the current value of the second argument
     * @return the new aggregate
     */
    A add(A aggregate, V1 val1, V2 val2);

    /**
     * Returns the result of the aggregate.
     *
     * @param aggregate the current aggregate
     * @return the result
     */
    R result(A aggregate);
}
