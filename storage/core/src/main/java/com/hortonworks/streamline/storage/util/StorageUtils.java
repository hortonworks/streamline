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

package com.hortonworks.streamline.storage.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.storage.annotation.StorableEntity;
import com.hortonworks.streamline.common.exception.DuplicateEntityException;
import com.hortonworks.streamline.storage.Storable;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static com.hortonworks.streamline.common.util.ReflectionHelper.getAnnotatedClasses;

/**
 * Utility methods for the storage package.
 */
public final class StorageUtils {


    private StorageUtils() {
    }

    public static <T extends Storable> T jsonToStorable(String json, Class<T> clazz) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, clazz);
    }

    public static String storableToJson(Storable storable) throws IOException {
        return storable != null ? new ObjectMapper().writeValueAsString(storable) : null;
    }

    public static void ensureUnique(Storable storable,
                                    Function<List<QueryParam>, Collection<? extends Storable>> listFn,
                                    List<QueryParam> queryParams) {
        Collection<? extends Storable> storables = listFn.apply(queryParams);
        Optional<Long> entities = storables.stream()
                .map(Storable::getId)
                .filter(x -> !x.equals(storable.getId()))
                .findAny();
        if (entities.isPresent()) {
            throw new DuplicateEntityException("Entity with '" + queryParams + "' already exists");
        }
    }

    @SuppressWarnings("unchecked")
    public static Collection<Class<? extends Storable>> getStreamlineEntities() {
        Set<Class<? extends Storable>> entities = new HashSet<>();
        getAnnotatedClasses("com.hortonworks", StorableEntity.class).forEach(clazz -> {
            if (Storable.class.isAssignableFrom(clazz)) {
                entities.add((Class<? extends Storable>) clazz);
            }
        });
        return entities;
    }
}
