/**
 *
 */
package com.hortonworks.streamline.streams.runtime.splitjoin;

import com.hortonworks.streamline.streams.StreamlineEvent;

/**
 * Joins the received {@link EventGroup} and generates a resultant {@link StreamlineEvent}
 *
 */
public interface Joiner {

    StreamlineEvent join(EventGroup eventGroup);
}
