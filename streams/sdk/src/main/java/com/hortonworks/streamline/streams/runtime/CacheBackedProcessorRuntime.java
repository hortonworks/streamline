/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.streams.runtime;

import com.hortonworks.streamline.cache.Cache;
import com.hortonworks.streamline.cache.view.config.CacheConfig;
import com.hortonworks.streamline.streams.Result;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.exception.ConfigException;
import com.hortonworks.streamline.streams.exception.ProcessingException;

import java.util.List;
import java.util.Map;

/**
 * Cache backed {@link CustomProcessorRuntime} that stores into the cache arbitrary values as
 * computed by {@link CacheBackedProcessorRuntime#getVal(StreamlineEvent, List)}
 *
 * @param <K> Type of the key used to cache values
 * @param <V> Type of the values to put/get into/from the cache
 */
public abstract class CacheBackedProcessorRuntime<K, V> implements CustomProcessorRuntime {
    private Cache<K, V> cache;

    @Override
    public final void initialize(Map<String, Object> config) {
        cache = getCacheFactory().create();
        initializeCustomProcessor(config);
    }

    /**
     * See {@link CustomProcessorRuntime#initialize(Map)}
     */
    public abstract void initializeCustomProcessor(Map<String, Object> config);

    /**
     * This final method provides a default implementation that calls the method
     * {@link CacheBackedProcessorRuntime#processResults(StreamlineEvent)}.The implementation
     * with the logic to compute the results should go in {@link CacheBackedProcessorRuntime#processResults(StreamlineEvent)}.
     */
    @Override
    public final List<Result> process(StreamlineEvent event) throws ProcessingException {
        List<Result> results;
        final K key = getKey(event);
        V val = cache.get(key);

        if (val == null) {                      // Not in cache
            results = processResults(event);    // compute results
            val = getVal(event, results);       // create value to store in cache
            cache.put(key, val);
        } else {
            results = getResultsForCachedValue(val);
        }
        return results;
    }

    /**
     * @param event to be processed.
     * @return The list wrapping the results of processing the event specified in argument
     */
    protected abstract List<Result> processResults(StreamlineEvent event);

    /**
     * Computes a list of results form the value in cache. If the list of values is stored in cache directly,
     * this method can simply return them
     * @param val the value stored into the cache
     * @return A list of results created from the value in cache.
     */
    protected abstract List<Result> getResultsForCachedValue(V val);

    /**
     * @param event for which to build the key that is to be used to store values in the cache
     * @return The key to be used to access cache
     */
    protected abstract K getKey(StreamlineEvent event);

    /**
     * Returns the value that is to be stored into the cache. This value can be the {@code List<Result>} as returned by
     * {@link CacheBackedProcessorRuntime#processResults(StreamlineEvent)},
     * or it can be any value that can be computed using the input arguments.
     * @param results List of results that is to be used to create the value to store into the cache. This is typically the result of
     *                {@link CacheBackedProcessorRuntime#processResults(StreamlineEvent)}
     * @param event that is to be used to create the value to store into the cache
     * @return The value to be stored into the cache.
     */
    protected abstract V getVal(StreamlineEvent event, List<Result> results);

    /**
     * @return A CacheFactory object that is used to create the cache instance used by this
     * {@link CacheBackedProcessorRuntime}
     */
    public abstract CacheFactory<K,V> getCacheFactory();

    // =====

    /**
     * Cache backed {@link CustomProcessorRuntime} that stores into the cache the results computed by
     * {@link CacheBackedProcessorRuntime#processResults(StreamlineEvent)}
     * @param <K> Type of the key used to cache values
     */
    public static abstract class CacheResults<K> extends CacheBackedProcessorRuntime<K, List<Result>> {
        @Override
        protected List<Result> getResultsForCachedValue(List<Result> results) {
            return results;
        }

        @Override
        protected List<Result> getVal(StreamlineEvent event, List<Result> results) {
            return results;
        }
    }

    // =====

    // TODO: This marker interface is here just for early code review feedback. It will
    // be part of the cache module. I am also going to try to provide a default implementation
    // that can be used to create the cache from it's configuration, without requiring the user
    // to have to know all the details of how to create a cache object.
    interface CacheFactory<K,V> {
        Cache<K,V> create();
    }
}
