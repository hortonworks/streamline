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
import java.util.List;

/**
 * Projection of fields in a rule
 */
public class Projection implements Serializable {
    private final List<Expression> expressions = new ArrayList<>();

    public Projection() {
    }

    public Projection(List<? extends Expression> expressions) {
        this.expressions.addAll(expressions);
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    public void setExpressions(List<? extends Expression> projectionExpressions) {
        this.expressions.addAll(projectionExpressions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Projection that = (Projection) o;

        return expressions != null ? expressions.equals(that.expressions) : that.expressions == null;

    }

    @Override
    public int hashCode() {
        return expressions != null ? expressions.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Projection{" +
                "expressions=" + expressions +
                '}';
    }
}
