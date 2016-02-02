package com.hortonworks.iotas.layout.runtime;

import com.hortonworks.iotas.common.IotasEvent;

import java.io.Serializable;
import java.util.List;

/**
 * Runtime abstraction for the action to be taken when a rule matches the condition
 */
public interface ActionRuntime extends Serializable {
    /**
     * Wraps the list of {@link IotasEvent} produced as a result
     * of applying the action and the output stream where this
     * has to be sent.
     */
    class Result {
        public final String stream;
        public final List<IotasEvent> events;
        /**
         * Create a new Result
         *
         * @param stream the stream where the result has to be sent
         * @param events the list of events in the result
         */
        public Result(String stream, List<IotasEvent> events) {
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

    /**
     * Execute the current action and return the Result object.
     * @param input the input IotasEvent
     * @return the result
     */
    Result execute(IotasEvent input);


    /**
     * The stream where the result of this action are sent out
     *
     * @return the stream where the result of this action are sent out
     */
    String getStream();

}
