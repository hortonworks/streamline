/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hortonworks.iotas.layout.design.rule.condition;

import com.hortonworks.iotas.common.Schema.Field;

import java.io.Serializable;
import java.util.List;

/**
 * This class represents a Rule Condition.
 */
public class Condition implements Serializable {
    private List<ConditionElement> conditionElements;

    public Condition() {
        // For JSON serializer
    }

    /** @return The collection of condition elements that define this condition */
    public List<ConditionElement> getConditionElements() {
        return conditionElements;
    }

    public void setConditionElements(List<ConditionElement> conditionElements) {
        this.conditionElements = conditionElements;
    }

    @Override
    public String toString() {
        return "Condition{" +
                "conditionElements=" + conditionElements +
                '}';
    }

    public static class ConditionElement implements Serializable {
        public enum Operation {EQUALS, NOT_EQUAL, GREATER_THAN, LESS_THAN, GREATER_THAN_EQUALS_TO, LESS_THAN_EQUALS_TO}   //TODO: Support BETWEEN ?

        public enum LogicalOperator {AND, OR}

        private Field firstOperand;                 // INTEGER i or STRING s or DOUBLE d...
        private Operation operation;                // EQUALS, NOT_EQUAL, GREATER_THAN,...
        private String secondOperand;               // It is a constant 2, 3.2, "Name",...
        private LogicalOperator logicalOperator;    // [AND, OR]  - Optional - it is null for the last ConditionElement

        public ConditionElement() {
            // For JSON serializer
        }

        /**
         * @return The first operand of this condition
         */
        public Field getFirstOperand() {
            return firstOperand;
        }

        
        public void setFirstOperand(Field firstOperand) {
            this.firstOperand = firstOperand;
        }

        /**
         * @return The operation applied
         */
        public Operation getOperation() {
            return operation;
        }

        
        public void setOperation(Operation operation) {
            this.operation = operation;
        }

        /**
         * @return The second operand of this condition. It is a constant.
         */
        public String getSecondOperand() {
            return secondOperand;
        }

        
        public void setSecondOperand(String secondOperand) {
            this.secondOperand = secondOperand;
        }

        /**
         * @return The logical operator that precedes the next condition element <br/>
         * null if it is the last condition element of the condition
         */
        public LogicalOperator getLogicalOperator() {
            return logicalOperator;
        }

        public void setLogicalOperator(LogicalOperator logicalOperator) {
            this.logicalOperator = logicalOperator;
        }

        @Override
        public String toString() {
            return "ConditionElement{" +
                    "firstOperand=" + firstOperand +
                    ", operation=" + operation +
                    ", secondOperand='" + secondOperand + '\'' +
                    ", logicalOperator=" + logicalOperator +
                    '}';
        }
    }
}
