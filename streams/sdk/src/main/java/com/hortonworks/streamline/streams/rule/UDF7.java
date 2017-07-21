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
 * This is an interface for implementing user defined functions with five arguments.
 *
 * @param <O> type of the result
 * @param <I1> type of the first input argument
 * @param <I2> type of the second input argument
 * @param <I3> type of the third input argument
 * @param <I4> type of the fourth input argument
 * @param <I5> type of the fifth input argument
 * @param <I6> type of the sixth input argument
 * @param <I7> type of the seventh input argument
 */
public interface UDF7<O, I1, I2, I3, I4, I5, I6, I7> {
    /**
     * Evaluate the inputs and return an output
     *
     * @param input1 the first input argument
     * @param input2 the second input argument
     * @param input3 the third input argument
     * @param input4 the fourth input argument
     * @param input5 the fifth input argument
     * @param input6 the sixth input argument
     * @param input7 the seventh input argument
     * @return the output
     */
    O evaluate(I1 input1, I2 input2, I3 input3, I4 input4, I5 input5, I6 input6, I7 input7);
}
