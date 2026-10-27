package ru.varino.lab1.resource;

import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionConflictMapperTest {
    @Test
    void recognizesJpaConflict() {
        assertTrue(TransactionConflictMapper.isOptimisticConflict(new OptimisticLockException()));
    }

    @Test
    void recognizesHibernateConflictWrappedAtCommit() {
        var conflict = new StaleObjectStateException("MovieNameRegistry", 1L);
        var failure = new RuntimeException("Could not commit transaction", new RuntimeException(conflict));
        assertTrue(TransactionConflictMapper.isOptimisticConflict(failure));
    }

    @Test
    void recognizesSuppressedConflict() {
        var failure = new RuntimeException("Rollback");
        failure.addSuppressed(new OptimisticLockException());
        assertTrue(TransactionConflictMapper.isOptimisticConflict(failure));
    }

    @Test
    void unrelatedFailuresAreNotReportedAsConflictsEvenWithCyclicCauses() {
        var first = new RuntimeException("Database unavailable");
        var second = new RuntimeException(first);
        first.initCause(second);
        assertFalse(TransactionConflictMapper.isOptimisticConflict(first));
    }
}
