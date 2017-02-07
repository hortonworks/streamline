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
