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
package com.hortonworks.streamline.streams.runtime.storm.grouping;

import org.apache.storm.generated.GlobalStreamId;
import org.apache.storm.grouping.CustomStreamGrouping;
import org.apache.storm.task.WorkerTopologyContext;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.runtime.storm.StreamlineRuntimeUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for handling fields grouping for components connected in streamline topologies.
 * All components in streamline topologies pass around StreamlineEvent objects. To support fields grouping
 * in storm, we will use this class to inspect the streamline event and group by the fields and send it to
 * the same downstream task
 */
public class FieldsGroupingAsCustomGrouping implements CustomStreamGrouping {
    private final List<String> groupingFields;
    private List<Integer> targetTasks;
    public FieldsGroupingAsCustomGrouping(List<String> groupingFields) {
        this.groupingFields = Collections.unmodifiableList(groupingFields);

    }
    @Override
    public void prepare(WorkerTopologyContext context, GlobalStreamId stream, List<Integer> targetTasks) {
        this.targetTasks = targetTasks;
    }

    @Override
    public List<Integer> chooseTasks(int taskId, List<Object> values) {
        List<Integer> result = new ArrayList<>();
        StreamlineEvent streamlineEvent = (StreamlineEvent) values.get(0);
        List<Object> groupByObjects = new ArrayList<>(groupingFields.size());
        for (String groupingField: groupingFields) {
            groupByObjects.add(StreamlineRuntimeUtil.getFieldValue(streamlineEvent, groupingField));
        }
        int taskIndex = Arrays.deepHashCode(groupByObjects.toArray()) % targetTasks.size();
        result.add(targetTasks.get(taskIndex));
        return result;
    }
}
