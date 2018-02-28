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
    MULTIPLY(1), DIVIDE(1),
    PLUS(2), MINUS(2),
    GREATER_THAN(3), LESS_THAN(3), GREATER_THAN_EQUALS_TO(3), LESS_THAN_EQUALS_TO(3),
    EQUALS(4), NOT_EQUAL(4),
    AND(5),
    OR(6);

    private final int precedence;

    Operator(int precedence) {
        this.precedence = precedence;
    }

    public int getPrecedence() {
        return precedence;
    }
}