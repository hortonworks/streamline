/**
 *
 */
package org.apache.streamline.streams.runtime.splitjoin;

import org.apache.streamline.streams.IotasEvent;

/**
 * Joins the received {@link EventGroup} and generates a resultant {@link IotasEvent}
 *
 */
public interface Joiner {

    IotasEvent join(EventGroup eventGroup);
}
