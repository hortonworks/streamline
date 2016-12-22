package org.apache.streamline.common.util;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ParallelStreamUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ParallelStreamUtil.class);

    public static <T> T execute(Supplier<T> supplier, Executor executor) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        LOG.debug("execute start");

        try {
            CompletableFuture<T> resultFuture = CompletableFuture.supplyAsync(supplier, executor);
            return resultFuture.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            handleExecutionException(e);
            // shouldn't reach here
            throw new IllegalStateException("Shouldn't reach here");
        } finally {
            LOG.debug("execute complete - elapsed: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            stopwatch.stop();
        }
    }

    private static void handleExecutionException(ExecutionException e) {
        Throwable t = e.getCause();
        if (t != null) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException(t);
            }
        } else {
            throw new RuntimeException(e);
        }
    }


}
