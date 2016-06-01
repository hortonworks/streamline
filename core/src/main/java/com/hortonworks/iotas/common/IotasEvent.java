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
     *
     * @return the key value map
     */
    Map<String, Object> getFieldsAndValues();


    /**
     * Auxiliary fields and values. Different Actions can add transformed or new fields and values without changing the original
     * {@code #getFieldsAndValues} received in this event.
     * For example, enrichment Action can add enrichments in the below format.
     * <p>
     * "enrichments": {
     *      "device-location":
     *          "device-address" : {
     *              "addr" : "5470, Great America Pkwy",
     *              "city" : "Santa Clara",
     *              "state" : "CA",
     *              "zip" : "95054",
     *              "country" : "US"
     *          }
     * }
     * </p>
     *
     * @return the auxiliary fields and values
     */
    Map<String, Object> getAuxiliaryFieldsAndValues();

    /**
     * Adds given {@code field} and {@code value} to auxiliary Map.
     */
    public void addAuxiliaryFieldAndValue(String field, Object value);

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
     *
     * @return stream name
     */
    String getSourceStream();
}
