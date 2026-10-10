package com.edu.unicauca.gasstation.backend.workers.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.RoleInfo;
import com.edu.unicauca.gasstation.backend.shifts.ShiftExternalService;
import com.edu.unicauca.gasstation.backend.workers.domain.models.Worker;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerChanges;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerDetail;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerFilter;
import com.edu.unicauca.gasstation.backend.workers.exception.DuplicateDocumentException;
import com.edu.unicauca.gasstation.backend.workers.exception.RoleNotFoundException;
import com.edu.unicauca.gasstation.backend.workers.exception.WorkerNotFoundException;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.WorkerRepository;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.WorkerSpecifications;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for workers (HU-9): create, edit, activate/deactivate, get one and list with filters.
 *
 * <p>Business rules applied here:
 * <ul>
 *   <li>The document is unique among workers, both when creating and when editing.</li>
 *   <li>The role must exist in the {@code shifts} module.</li>
 *   <li>There is no delete: a worker is deactivated.</li>
 * </ul>
 * Trimming spaces and starting active are rules of the {@link Worker} model itself.
 * Every result carries the role read from {@code shifts} ({@link WorkerDetail}).
 */
@Service
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final ShiftExternalService shiftExternalService;

    /**
     * Registers a new worker, always active.
     *
     * @throws DuplicateDocumentException if another worker already has the document
     * @throws RoleNotFoundException      if the role does not exist
     */
    @Transactional
    public WorkerDetail create(Worker newWorker) {
        if (workerRepository.existsByDocument(newWorker.getDocument())) {
            throw new DuplicateDocumentException(newWorker.getDocument());
        }
        RoleInfo role = findRole(newWorker.getRoleId());
        return new WorkerDetail(saveChecked(newWorker), role);
    }

    /**
     * Replaces name, document and role of a worker. Inactive workers can be edited too.
     *
     * @param changes new name, document and role, already without leading or trailing spaces
     * @throws WorkerNotFoundException    if the worker does not exist
     * @throws DuplicateDocumentException if a different worker already has the document
     * @throws RoleNotFoundException      if the role does not exist
     */
    @Transactional
    public WorkerDetail update(UUID id, WorkerChanges changes) {
        Worker worker = findWorker(id);
        if (workerRepository.existsByDocumentAndIdNot(changes.document(), id)) {
            throw new DuplicateDocumentException(changes.document());
        }
        RoleInfo role = findRole(changes.roleId());

        worker.updateDetails(changes.fullName(), changes.document(), role.id());
        return new WorkerDetail(saveChecked(worker), role);
    }

    /**
     * Activates or deactivates a worker. Setting the state it already has is not an error.
     *
     * @throws WorkerNotFoundException if the worker does not exist
     */
    @Transactional
    public WorkerDetail changeStatus(UUID id, boolean active) {
        Worker worker = findWorker(id);
        if (active) {
            worker.activate();
        } else {
            worker.deactivate();
        }
        return new WorkerDetail(workerRepository.save(worker), findRole(worker.getRoleId()));
    }

    /**
     * Returns one worker, active or not.
     *
     * @throws WorkerNotFoundException if the worker does not exist
     */
    @Transactional(readOnly = true)
    public WorkerDetail getById(UUID id) {
        Worker worker = findWorker(id);
        return new WorkerDetail(worker, findRole(worker.getRoleId()));
    }

    /**
     * Lists workers ordered by full name. Every filter is optional and they can be combined.
     */
    @Transactional(readOnly = true)
    public List<WorkerDetail> list(WorkerFilter filter) {
        List<Worker> workers = workerRepository.findAll(WorkerSpecifications.matching(filter), Sort.by("fullName"));

        // A single call to shifts for every role in the list, not one per worker
        Set<UUID> roleIds = workers.stream().map(Worker::getRoleId).collect(Collectors.toSet());
        Map<UUID, RoleInfo> roles = shiftExternalService.getRolesByIds(roleIds);

        return workers.stream()
                .map(worker -> new WorkerDetail(worker, requireRole(roles, worker)))
                .toList();
    }

    /**
     * Saves with {@code saveAndFlush} so the INSERT/UPDATE is sent now: if two concurrent requests passed the
     * document check, the database UNIQUE constraint rejects the second one here and it becomes a 409.
     */
    private Worker saveChecked(Worker worker) {
        try {
            return workerRepository.saveAndFlush(worker);
        } catch (DataIntegrityViolationException ex) {
            String cause = ex.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains(Worker.UNIQUE_DOCUMENT_CONSTRAINT)) {
                throw new DuplicateDocumentException(worker.getDocument());
            }
            throw ex;
        }
    }

    private Worker findWorker(UUID id) {
        return workerRepository.findById(id).orElseThrow(() -> new WorkerNotFoundException(id));
    }

    private RoleInfo findRole(UUID roleId) {
        RoleInfo role = shiftExternalService.getRolesByIds(Set.of(roleId)).get(roleId);
        if (role == null) {
            throw new RoleNotFoundException(roleId);
        }
        return role;
    }

    private static RoleInfo requireRole(Map<UUID, RoleInfo> roles, Worker worker) {
        RoleInfo role = roles.get(worker.getRoleId());
        if (role == null) {
            // Cannot happen while the FK worker.role_id -> role.id exists
            throw new IllegalStateException("Role " + worker.getRoleId() + " of worker " + worker.getId() + " not found");
        }
        return role;
    }
}
