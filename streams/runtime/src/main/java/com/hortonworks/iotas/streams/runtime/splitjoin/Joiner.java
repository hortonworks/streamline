/**
 *
 */
package com.hortonworks.iotas.streams.runtime.splitjoin;

import com.hortonworks.iotas.streams.IotasEvent;

/**
 * Joins the received {@link EventGroup} and generates a resultant {@link IotasEvent}
 *
 */
public interface Joiner {

    IotasEvent join(EventGroup eventGroup);
}
