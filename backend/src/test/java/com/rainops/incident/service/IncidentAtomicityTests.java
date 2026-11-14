package com.rainops.incident.service;

import com.rainops.incident.entity.*;
import com.rainops.incident.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class IncidentAtomicityTests {
    @Autowired IncidentService service;
    @Autowired IncidentRepository incidents;
    @Autowired IncidentStatusHistoryRepository history;
    @Autowired PlatformTransactionManager transactionManager;

    @Test void historyFailureRollsBackStatusAndResolutionTime() {
        UUID id = create();
        try {
            service.changeStatus(id, IncidentStatus.IN_PROGRESS, "Started");
            assertThatThrownBy(() -> service.changeStatus(id, IncidentStatus.RESOLVED, "x".repeat(1001)))
                .isInstanceOf(DataIntegrityViolationException.class);
            Incident stored = service.getIncident(id);
            assertThat(stored.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
            assertThat(stored.getResolvedAt()).isNull();
            assertThat(service.getHistory(id)).hasSize(2);
        } finally { cleanup(id); }
    }

    @Test void concurrentIdenticalTransitionsProduceOneHistoryEntry() throws Exception {
        UUID id = create();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Incident> change = () -> {
                start.await();
                return service.changeStatus(id, IncidentStatus.IN_PROGRESS, "Concurrent request");
            };
            Future<Incident> first = pool.submit(change);
            Future<Incident> second = pool.submit(change);
            start.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS).getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
            assertThat(second.get(15, TimeUnit.SECONDS).getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
            assertThat(service.getHistory(id)).hasSize(2);
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
            cleanup(id);
        }
    }

    private UUID create() {
        return service.createIncident("Atomicity test", "Description", "District", "Entrance",
                null, null, IncidentPriority.HIGH).getId();
    }

    private void cleanup(UUID id) {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            history.deleteAll(history.findByIncidentIdOrderByChangedAtAscIdAsc(id));
            history.flush();
            incidents.deleteById(id);
        });
    }
}
