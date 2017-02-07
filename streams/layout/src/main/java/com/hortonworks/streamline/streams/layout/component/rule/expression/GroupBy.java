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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A group by expression
 */
public class GroupBy implements Serializable {
    private List<Expression> expressions = new ArrayList<>();

    // for jackson
    public GroupBy() {
    }

    public GroupBy(Expression expression) {
        this.expressions.add(expression);
    }

    public GroupBy(List<Expression> expressions) {
        this.expressions.addAll(expressions);
    }

    public List<Expression> getExpressions() {
        return Collections.unmodifiableList(expressions);
    }

    public void setExpression(List<Expression> expressions) {
        this.expressions = new ArrayList<>(expressions);
    }

    @Override
    public String toString() {
        return "GroupBy{" +
                "expressions=" + expressions +
                '}';
    }
}
