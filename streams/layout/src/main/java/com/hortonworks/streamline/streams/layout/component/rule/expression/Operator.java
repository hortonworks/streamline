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
package com.hortonworks.streamline.streams.layout.component.rule.expression;

/**
 * Supported operators
 */
public enum Operator {
    GREATER_THAN(1), LESS_THAN(1), GREATER_THAN_EQUALS_TO(1), LESS_THAN_EQUALS_TO(1),
    EQUALS(2), NOT_EQUAL(2),
    AND(3),
    OR(4);

    private final int precedence;

    Operator(int precedence) {
        this.precedence = precedence;
    }

    public int getPrecedence() {
        return precedence;
    }
}