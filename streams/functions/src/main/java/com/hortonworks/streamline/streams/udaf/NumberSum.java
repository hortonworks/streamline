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
package com.hortonworks.streamline.streams.udaf;

import com.hortonworks.streamline.streams.rule.UDAF;

public class NumberSum implements UDAF<Number, Number, Number> {
    @Override
    public Number init() {
        return 0;
    }

    @Override
    public Number add(Number aggregate, Number val) {
        if (val instanceof Byte) {
            return (byte) (aggregate.byteValue() + val.byteValue());
        } else if (val instanceof Short) {
            return (short) (aggregate.shortValue() + val.shortValue());
        } else if (val instanceof Integer) {
            return aggregate.intValue() + val.intValue();
        } else if (val instanceof Long) {
            return aggregate.longValue() + val.longValue();
        } else if (val instanceof Float) {
            return aggregate.floatValue() + val.floatValue();
        } else if (val instanceof Double) {
            return aggregate.doubleValue() + val.doubleValue();
        }
        throw new IllegalArgumentException("Value type " + val.getClass());
    }

    @Override
    public Number result(Number aggregate) {
        return aggregate;
    }

}
