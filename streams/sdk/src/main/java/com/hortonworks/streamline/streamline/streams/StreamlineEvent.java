package org.apache.streamline.streams;

import java.util.Map;

/**
 * Represents the event that flows from the source to the rule engine.
 * This can also be referred in the notification to know which events produced
 * the notification.
 */
public interface StreamlineEvent  extends Map<String,Object> {

    // Default value chosen to be blank and not the default used in storm since wanted to keep it independent of storm.
    String DEFAULT_SOURCE_STREAM = "default";// Default value chosen to be blank and not the default used in storm since wanted to keep it independent of storm.

    String STREAMLINE_EVENT = "streamline-event"; // do not use . or _ in key names

    String PRIMITIVE_PAYLOAD_FIELD = "org.apache.streamline.streams.payload.primitive.field";

    String NESTED_FIELD_SPLIT_REGEX = "\\.";

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
    void addAuxiliaryFieldAndValue(String field, Object value);

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

    /**
     * Returns a new Streamline event with the given fieldsAndValues added to the existing fieldsAndValues.
     * All the other fields are copied from this event.
     *
     * @param fieldsAndValues the map of fieldsAndValues to add
     * @return the new StreamlineEvent
     */
    StreamlineEvent addFieldsAndValues(Map<String, Object> fieldsAndValues);

    /**
     * Returns a new Streamline event with the given headers added to the existing headers.
     * All the other fields are copied from this event.
     *
     * @param headers the map of fieldsAndValues to add or overwrite
     * @return the new StreamlineEvent
     */
    StreamlineEvent addHeaders(Map<String, Object> headers);

    /**
     * Returns the byte representation of this event so that it can be stored to a store like HDFS
     *
     * @return
     */
    byte[] getBytes();
}
