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
 * This is an interface for implementing user defined functions with two arguments.
 *
 * @param <O> type of the result
 * @param <I1> type of the first input argument
 * @param <I2> type of the second input argument
 */
public interface UDF2<O, I1, I2> {
    /**
     * Evaluate the inputs and return an output
     *
     * @param input1 the first input argument
     * @param input2 the second input argument
     * @return the output
     */
    O evaluate(I1 input1, I2 input2);
}
