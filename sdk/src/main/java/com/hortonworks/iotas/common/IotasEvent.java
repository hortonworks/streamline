package com.hortonworks.iotas.common;

import java.util.Map;

/**
 * Represents the event that flows from the source to the rule engine.
 * This can also be referred in the notification to know which events produced
 * the notification.
 */
public interface IotasEvent {

    String IOTAS_EVENT = "iotas.event";

    /**
     * The key values in the event.
     * @return the key value map
     */
    Map<String, Object> getFieldsAndValues();


    /**
     * The event header that represents some meta data about
     * this event. This is optional and by default empty.
     *
     * @return the key value map representing the event header
     */
    Map<String, Object> getHeader();

    /**
     * The unique event id.
     *
     * @return the id
     */
    String getId();

    /**
     * The unique id of the data source that produced this event.
     *
     * @return the data source id
     */
    String getDataSourceId();

    /**
     * The source stream that generated this event
     * @return stream name
     */
    String getSourceStream ();
}
