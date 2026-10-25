package ru.varino.lab1.service;

import org.junit.jupiter.api.Test;
import ru.varino.lab1.entity.ImportOperation;
import ru.varino.lab1.repository.ImportRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryServiceTest {
    @Test
    void normalUserReceivesOnlyOwnHistoryAndAdminReceivesAll() throws Exception {
        ImportOperation alice = new ImportOperation();
        alice.setUsername("alice");
        ImportOperation bob = new ImportOperation();
        bob.setUsername("bob");
        var history = new HistoryService();
        var field = HistoryService.class.getDeclaredField("importRepository");
        field.setAccessible(true);
        field.set(history, new ImportRepository() {
            @Override
            public List<ImportOperation> findAll() { return List.of(alice, bob); }

            @Override
            public List<ImportOperation> findByUsername(String username) {
                assertEquals("alice", username);
                return List.of(alice);
            }
        });
        assertEquals(List.of(alice), history.findForUser("alice", false));
        assertEquals(List.of(alice, bob), history.findForUser("alice", true));
    }
}
