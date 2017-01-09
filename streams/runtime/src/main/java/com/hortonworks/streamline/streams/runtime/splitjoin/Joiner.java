/**
 *
 */
package org.apache.streamline.streams.runtime.splitjoin;

import org.apache.streamline.streams.StreamlineEvent;

/**
 * Joins the received {@link EventGroup} and generates a resultant {@link StreamlineEvent}
 *
 */
public interface Joiner {

    StreamlineEvent join(EventGroup eventGroup);
}
