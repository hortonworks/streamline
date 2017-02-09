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

import java.util.List;

/**
 * For now this exists to differentiate between regular and aggregate functions so that
 * we can ensure the projection list does not contain non-aggregate functions when a group by is involved.
 */
public class AggregateFunctionExpression extends FunctionExpression {
    /**
     * Built in aggregate function (e.g. built in sql aggregate functions like MAX, COUNT etc)
     */
    public AggregateFunctionExpression(String name, List<? extends Expression> operands) {
        super(name, operands);
    }

    /**
     * User defined aggregate function
     */
    public AggregateFunctionExpression(String name, String className, List<? extends Expression> operands) {
        super(name, className, operands);
    }

    // for jackson
    private AggregateFunctionExpression() {
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
