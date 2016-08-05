package com.hortonworks.iotas.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A file watcher service for iotas that can be used to dispatch file event notifications
 * to different handlers.
 */
public class FileWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(FileWatcher.class);
    private WatchService watchService = null;
    private final List<FileEventHandler> fileEventHandlers;
    private Map<WatchKey, FileEventHandler> watchKeyFileEventHandlerMap = new HashMap<>();
    private Map<WatchKey, Path> watchKeyPathMap = new HashMap<>();

    public FileWatcher (List<FileEventHandler> fileEventHandlers) {
        this.fileEventHandlers = fileEventHandlers;
    }

    /**
     * Blocking method to check and dispatch file events.
     * @return Returns false if file watcher will not receive any more events to indicate caller to break out of loop
     */
    public boolean processEvents() {
        if (watchKeyPathMap.isEmpty()) {
            return false;
        }
        // wait for key to be signalled
        WatchKey key;
        try {
            key = watchService.take();
        } catch (ClosedWatchServiceException|InterruptedException ex) {
            LOG.info("Watch service interrupted or closed while waiting to get next watch key. Exiting!", ex);
            return false;
        }
        Path dir = watchKeyPathMap.get(key);
        if (dir == null) {
            LOG.info("Unrecognized watch key: " + key + ". Skipping the key without reseting it.");
            return true;
        }
        for (WatchEvent<?> event: key.pollEvents()) {
            WatchEvent.Kind kind = event.kind();
            if (kind == StandardWatchEventKinds.OVERFLOW) {
                LOG.warn("Overflow event received for key: " + key + ". This means events have been missed or discarded. Please verify.");
                return true;
            }
            // Context for directory entry event is the file name of entry
            WatchEvent<Path> ev = (WatchEvent<Path>) event;
            Path name = ev.context();
            Path child = dir.resolve(name);
            LOG.info(String.format("%s: %s\n", event.kind().name(), child));
            try {
                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    watchKeyFileEventHandlerMap.get(key).created(child);
                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    watchKeyFileEventHandlerMap.get(key).modified(child);
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    watchKeyFileEventHandlerMap.get(key).deleted(child);
                }
            } catch (Exception ex) {
                LOG.warn(String.format("Exception thrown by handler %s while processing event %s", watchKeyFileEventHandlerMap.get(key), event.kind().name()));
            }
        }
        // reset key and remove from set if directory no longer accessible
        boolean valid = key.reset();
        if (!valid) {
            LOG.info("Key " + key + " not being watched any more as it could not be reset.");
            watchKeyPathMap.remove(key);
            watchKeyFileEventHandlerMap.remove(key);

        }
        return true;
    }

    public void register () {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            String message = "Could not register %s with file watch service.";
            if (fileEventHandlers != null && fileEventHandlers.size() > 0) {
                for (FileEventHandler fileEventHandler: fileEventHandlers) {
                    try {
                        Path path = Paths.get(fileEventHandler.getDirectoryToWatch());
                        WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
                                StandardWatchEventKinds.ENTRY_DELETE);
                        watchKeyFileEventHandlerMap.put(watchKey, fileEventHandler);
                        watchKeyPathMap.put(watchKey, path);
                        LOG.info("Successfully registered " + fileEventHandler.getDirectoryToWatch() + " with file watch service.");
                    } catch (NotDirectoryException e) {
                        LOG.warn(String.format(message, fileEventHandler.getDirectoryToWatch()) + "Reason: Not a directory", e);
                    } catch (RuntimeException|IOException e) {
                        LOG.warn(String.format(message, fileEventHandler.getDirectoryToWatch()), e);
                    }
                }
                if (watchKeyFileEventHandlerMap.isEmpty()) {
                    LOG.warn("Could not register any file event handler successfully.");
                }
            } else {
                LOG.info("No file event handlers passed.");
            }
        } catch (IOException e) {
            LOG.warn("Unable to get the default file system watch service. Iotas FileWatcher is not active.", e);
        }
    }
}
