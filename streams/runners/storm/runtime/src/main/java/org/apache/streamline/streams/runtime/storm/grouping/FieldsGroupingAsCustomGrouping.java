package org.apache.streamline.streams.runtime.storm.grouping;

import org.apache.storm.generated.GlobalStreamId;
import org.apache.storm.grouping.CustomStreamGrouping;
import org.apache.storm.task.WorkerTopologyContext;
import org.apache.streamline.streams.StreamlineEvent;

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
    private static final String NESTED_FIELD_SPLIT_REGEX = "\\.";
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
            groupByObjects.add(getGroupingValue(streamlineEvent, groupingField));
        }
        int taskIndex = Arrays.deepHashCode(groupByObjects.toArray()) % targetTasks.size();
        result.add(targetTasks.get(taskIndex));
        return result;
    }

    private static Object getGroupingValue(StreamlineEvent streamlineEvent, String groupingField) {
        Map fieldValues = streamlineEvent;
        String[] nestedKeys = groupingField.split(NESTED_FIELD_SPLIT_REGEX);
        for (int i = 0; i < (nestedKeys.length - 1); ++i) {
            if (fieldValues == null)
                break;
            fieldValues = (Map) fieldValues.get(nestedKeys[i]);
        }
        return fieldValues != null ? fieldValues.get(nestedKeys[nestedKeys.length - 1]) : null;
    }
}
