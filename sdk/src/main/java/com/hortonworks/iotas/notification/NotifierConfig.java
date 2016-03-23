package com.hortonworks.iotas.notification;

import java.util.Map;
import java.util.Properties;

/**
 * A set of notifier specific properties and
 * field values (defaults plus user configured) for
 * the notifier fields.
 */
public interface NotifierConfig {

    /**
     * The class name of this notifier
     */
    String getClassName();

    /**
     * The path of the jar containing the notifier.
     */
    String getJarPath();

    /**
     * The notifier specific properties.
     * E.g. SMTP server, port for email notifier.
     *
     * @return the notifier properties
     */
    Properties getProperties();

    /**
     * The defaults for some of the notifier fields.
     *
     * @return a map of defaults for some of the notifier fields.
     */
    Map<String, String> getDefaultFieldValues();

}
