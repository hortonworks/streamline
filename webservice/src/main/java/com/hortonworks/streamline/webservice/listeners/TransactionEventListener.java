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

package com.hortonworks.streamline.webservice.listeners;

import com.hortonworks.registries.common.transaction.TransactionIsolation;
import com.hortonworks.registries.common.transaction.UnitOfWork;
import com.hortonworks.registries.storage.TransactionManager;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TransactionEventListener implements ApplicationEventListener {

    private final ConcurrentMap<ResourceMethod, Optional<UnitOfWork>> methodMap = new ConcurrentHashMap<>();
    private final TransactionManager transactionManager;

    public TransactionEventListener(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    private static class UnitOfWorkEventListener implements RequestEventListener {
        private final ConcurrentMap<ResourceMethod, Optional<UnitOfWork>> methodMap;
        private final TransactionManager transactionManager;
        private boolean useTransactionForUnitOfWork = true;

        public UnitOfWorkEventListener(ConcurrentMap<ResourceMethod, Optional<UnitOfWork>> methodMap, TransactionManager transactionManager) {
            this.methodMap = methodMap;
            this.transactionManager = transactionManager;
        }

        @Override
        public void onEvent(RequestEvent event) {
            final RequestEvent.Type eventType = event.getType();
            if (eventType == RequestEvent.Type.RESOURCE_METHOD_START) {
                Optional<UnitOfWork> unitOfWork = methodMap.computeIfAbsent(event.getUriInfo()
                        .getMatchedResourceMethod(), UnitOfWorkEventListener::registerUnitOfWorkAnnotations);
                useTransactionForUnitOfWork = unitOfWork.isPresent() ? unitOfWork.get().transactional() : true;
                TransactionIsolation transactionIsolation = unitOfWork.isPresent() ? unitOfWork.get().transactionIsolation() : TransactionIsolation.DEFAULT;
                if (useTransactionForUnitOfWork)
                    transactionManager.beginTransaction(transactionIsolation);
            } else if (eventType == RequestEvent.Type.RESP_FILTERS_START) {
                // not supporting transactions to filters
            } else if (eventType == RequestEvent.Type.ON_EXCEPTION) {
                if (useTransactionForUnitOfWork)
                    transactionManager.rollbackTransaction();
            } else if (eventType == RequestEvent.Type.FINISHED) {
                if (useTransactionForUnitOfWork && event.isSuccess())
                    transactionManager.commitTransaction();
            }
        }

        private static Optional<UnitOfWork> registerUnitOfWorkAnnotations(ResourceMethod method) {
            UnitOfWork annotation = method.getInvocable().getDefinitionMethod().getAnnotation(UnitOfWork.class);
            if (annotation == null) {
                annotation = method.getInvocable().getHandlingMethod().getAnnotation(UnitOfWork.class);
            }
            return Optional.ofNullable(annotation);
        }
    }

    @Override
    public void onEvent(ApplicationEvent applicationEvent) {

    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        return new UnitOfWorkEventListener(methodMap, transactionManager);
    }
}