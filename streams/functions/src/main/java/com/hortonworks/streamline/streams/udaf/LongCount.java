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

public class LongCount implements UDAF<Long, Object, Long> {
    @Override
    public Long init() {
        return 0L;
    }

    @Override
    public Long add(Long aggregate, Object val) {
        if (val == null) {
            return aggregate;
        } else if (val instanceof Iterable) {
            for (Object o : (Iterable<?>) val) {
                if (o == null) {
                    return aggregate;
                }
            }
        }
        return aggregate + 1;
    }

    @Override
    public Long result(Long aggregate) {
        return aggregate;
    }
}
