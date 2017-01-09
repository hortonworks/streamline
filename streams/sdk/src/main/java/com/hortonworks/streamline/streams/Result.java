package org.apache.streamline.streams;

import java.util.List;

/**
 * Wraps the list of {@link StreamlineEvent} produced as a result
 * of applying the action and the output stream where this
 * has to be sent.
 */
public class Result {
    public final String stream;
    public final List<StreamlineEvent> events;
    /**
     * Create a new Result
     *
     * @param stream the stream where the result has to be sent
     * @param events the list of events in the result
     */
    public Result(String stream, List<StreamlineEvent> events) {
        this.stream = stream;
        this.events = events;
    }

    @Override
    public String toString() {
        return "Result{" +
                "stream='" + stream + '\'' +
                ", events=" + events +
                '}';
    }
}
