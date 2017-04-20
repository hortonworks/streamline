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
package com.hortonworks.streamline.common.security.authenticator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.sasl.RealmCallback;
import java.util.Map;

/**
 * Base login class that implements login based on JAAS config
 */
public abstract class AbstractLogin implements Login {
    private static final Logger log = LoggerFactory.getLogger(AbstractLogin.class);

    protected String loginContextName;
    protected LoginContext loginContext;
    protected static final String JAAS_CONFIG_SYSTEM_PROPERTY = "java.security.auth.login.config";

    @Override
    public void configure(Map<String, ?> configs, String loginContextName) {
        this.loginContextName = loginContextName;
    }

    @Override
    public LoginContext login() throws LoginException {
        String jaasConfigFile = System.getProperty(JAAS_CONFIG_SYSTEM_PROPERTY);
        if (jaasConfigFile == null) {
            log.debug("System property " + JAAS_CONFIG_SYSTEM_PROPERTY + " for jaas config file is not set, using default JAAS configuration.");
        }
        loginContext = new LoginContext(loginContextName, new LoginCallbackHandler());
        loginContext.login();
        log.info("Successfully logged in.");
        return loginContext;
    }

    /**
     * Callback handler for creating login context. Login callback handlers
     * should support the callbacks required for the login modules corresponding
     * to contexts. By default, we don't not support callback handlers which
     * require additional user input.
     */
    public static class LoginCallbackHandler implements CallbackHandler {

        @Override
        public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    NameCallback nc = (NameCallback) callback;
                    nc.setName(nc.getDefaultName());
                } else if (callback instanceof PasswordCallback) {
                    String errorMessage = "Could not login: the client is being asked for a password, but this " +
                            " module does not currently support obtaining a password from the user.";
                    throw new UnsupportedCallbackException(callback, errorMessage);
                } else if (callback instanceof RealmCallback) {
                    RealmCallback rc = (RealmCallback) callback;
                    rc.setText(rc.getDefaultText());
                } else {
                    throw new UnsupportedCallbackException(callback, "Unrecognized Login callback");
                }
            }
        }
    }
}
