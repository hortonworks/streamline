/**
 * Copyright 2017 Hortonworks.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.hortonworks.streamline.storage.transaction;

import com.hortonworks.streamline.storage.exception.IgnoreTransactionRollbackException;

public class ManagedTransactionTestHelper {
    class IntendedException extends Exception {
        public IntendedException() {
            super("Intended exception!");
        }
    }

    private boolean called = false;
    private Object[] calledArgs;

    public void call(Object...args) {
        this.calledArgs = args;
        called = true;
    }

    public void throwException(Object...args) throws Exception {
        call(args);
        throw new IntendedException();
    }

    public void throwIgnoreRollbackException(Object...args) throws Exception {
        call(args);
        throw new IgnoreTransactionRollbackException(new IntendedException());
    }

    public boolean isCalled() {
        return called;
    }

    public Object[] getCalledArgs() {
        return calledArgs;
    }

    public void reset() {
        called = false;
    }
}
