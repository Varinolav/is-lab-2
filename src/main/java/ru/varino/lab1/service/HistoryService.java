package ru.varino.lab1.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.varino.lab1.entity.ImportOperation;
import ru.varino.lab1.entity.ImportStatus;
import ru.varino.lab1.repository.ImportRepository;

import java.time.LocalDateTime;
import java.util.List;

import static jakarta.transaction.Transactional.TxType.MANDATORY;
import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;

@ApplicationScoped
public class HistoryService {
    @Inject
    private ImportRepository importRepository;

    @Transactional
    public void saveSuccess(String username, long count) {
        save(username, ImportStatus.SUCCESS, count);
    }

    @Transactional(REQUIRES_NEW)
    public void saveFailed(String username) {
        save(username, ImportStatus.FAILED, null);
    }

    @Transactional
    public List<ImportOperation> findForUser(String username, boolean isAdmin) {
        return isAdmin ? importRepository.findAll() : importRepository.findByUsername(username);
    }

    @Transactional
    private void save(String username, ImportStatus status, Long count) {
        ImportOperation operation = new ImportOperation();
        operation.setUsername(username);
        operation.setStatus(status);
        operation.setCount(count);
        operation.setCreationDate(LocalDateTime.now());
        importRepository.save(operation);
    }

}
