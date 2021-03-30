package eu.xenit.alfresco.healthprocessor.processing;

import eu.xenit.alfresco.healthprocessor.util.TransactionHelper;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.lock.LockAcquisitionException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

/**
 * Responsibilities: upon trigger ({@link #startIfNotRunning()}) decide if the processor should be triggered, trigger
 * processor, persist state so other Alfresco nodes can decide if the processor should be triggered.
 */
@RequiredArgsConstructor
@Slf4j
public class ProcessorTask {

    static final long LOCK_TTL = 5000L;
    static final QName LOCK_QNAME = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "HealthProcessor");

    private final ProcessorConfiguration configuration;
    private final ProcessorService processorService;
    private final TransactionHelper transactionHelper;
    private final JobLockService jobLockService;

    @SuppressWarnings("unused")
    public void startIfNotRunning() {
        AuthenticationUtil.runAs(() -> {
            startIfNotRunningAsUser();
            return null;
        }, configuration.getRunAsUser());
    }

    void startIfNotRunningAsUser() {
        log.debug("Health-Processor initializing as user: {}", AuthenticationUtil.getRunAsUser());
        transactionHelper.inTransaction(this::startIfNotRunningAsUserInTransaction, configuration.isReadOnly());
    }

    void startIfNotRunningAsUserInTransaction() {
        if (!configuration.isSingleTenant()) {
            start();
            return;
        }

        LockCallback lockCallback = new LockCallback();
        try {
            String lockToken = jobLockService.getLock(LOCK_QNAME, LOCK_TTL, lockCallback);
            log.debug("Successfully claimed job lock. QName: {}, TTL: {}, token: {}", LOCK_QNAME, LOCK_TTL, lockToken);

            start();
        } catch (LockAcquisitionException e) {
            log.info("Health-Processor already active on other node, skipping...");
            log.debug("Exception thrown while trying to claim lock '{}'", LOCK_QNAME, e);
        } finally {
            lockCallback.running.set(false);
        }
    }

    private void start() {
        processorService.execute();
    }

    private static class LockCallback implements JobLockService.JobLockRefreshCallback {

        final AtomicBoolean running = new AtomicBoolean(true);

        @Override
        public boolean isActive() {
            return running.get();
        }

        @Override
        public void lockReleased() {
            running.set(false);
        }
    }

}
