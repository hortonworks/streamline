package com.hortonworks.streamline.webservice.transaction;

import com.hortonworks.streamline.storage.CacheBackedStorageManager;
import com.hortonworks.streamline.storage.StorageManager;
import com.hortonworks.streamline.storage.TransactionalStorageManager;
import com.hortonworks.streamline.storage.impl.jdbc.transaction.TransactionManager;
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
                if (useTransactionForUnitOfWork)
                    transactionManager.beginTransaction();
            } else if (eventType == RequestEvent.Type.RESP_FILTERS_START) {
                // not supporting transactions to filters
            } else if (eventType == RequestEvent.Type.ON_EXCEPTION) {
                if (useTransactionForUnitOfWork)
                    transactionManager.rollbackTransaction();
            } else if (eventType == RequestEvent.Type.FINISHED) {
                if (useTransactionForUnitOfWork)
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
