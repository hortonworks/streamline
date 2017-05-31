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

import com.hortonworks.streamline.streams.rule.UDAF2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Computes streaming top n values of a group of values
 */
public class Topn<T extends Comparable<T>> implements UDAF2<PriorityQueue<T>, Integer, T, List<T>> {
    @Override
    public PriorityQueue<T> init() {
        return new PriorityQueue<>();
    }

    @Override
    public PriorityQueue<T> add(PriorityQueue<T> aggregate, Integer n, T val) {
        if (n <= 0) {
            return aggregate;
        }
        if (aggregate.size() >= n) {
            if (val.compareTo(aggregate.peek()) > 0) {
                aggregate.remove();
                aggregate.add(val);
            }
        } else {
            aggregate.add(val);
        }
        return aggregate;

    }

    @Override
    public List<T> result(PriorityQueue<T> aggregate) {
        List<T> res = new ArrayList<>(aggregate);
        res.sort(Comparator.reverseOrder());
        return res;
    }
}
