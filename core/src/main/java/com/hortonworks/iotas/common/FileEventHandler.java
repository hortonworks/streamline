package com.hortonworks.iotas.common;

import java.nio.file.Path;

/**
 * An interface expected to be implemented by any Iotas component that needs to handle file watcher events.
 * Currently supported events are create, delete and modify
 */
public interface FileEventHandler {
    /**
     *
     * @return String representing path of the directory to be watched for file events
     */
    String getDirectoryToWatch ();

    /**
     * Handle the file created
     * @param path Path to the file created
     */
    void created (Path path);

    /**
     * Handle the file modified
     * @param path Path to the file modified
     */
    void modified (Path path);

    /**
     * Handle the file deleted
     * @param path Path to the file deleted
     */
    void deleted (Path path);
}
