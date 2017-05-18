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
package com.hortonworks.streamline.streams.security;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public interface StreamlineAuthorizer {
    /**
     * Initialize the authorizer
     */
    void init(Map<String, Object> config);

    /**
     * Check if the authenticated user has given permission on the target entity
     * identified by the given targetEntityId and targetEntityNamespace
     */
    boolean hasPermissions(AuthenticationContext ctx, String targetEntityNamespace, Long targetEntityId, EnumSet<Permission> permissions);

    /**
     * Check if the authenticated user belongs to a role
     */
    boolean hasRole(AuthenticationContext ctx, String role);

    /**
     * Grant permissions for the currently authenticated user on the target entity
     */
    void addAcl(AuthenticationContext ctx, String targetEntityNamespace, Long targetEntityId, boolean owner, boolean grant, EnumSet<Permission> permissions);

    /**
     * Remove permissions for the currently authenticated user on the target entity
     */
    void removeAcl(AuthenticationContext ctx, String targetEntityNamespace, Long targetEntityId);
}
