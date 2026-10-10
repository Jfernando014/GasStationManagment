package com.edu.unicauca.gasstation.backend.workers.application;

import com.edu.unicauca.gasstation.backend.shifts.RoleInfo;
import com.edu.unicauca.gasstation.backend.shifts.ShiftExternalService;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.CreateWorkerRequest;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.UpdateWorkerRequest;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.WorkerResponse;
import com.edu.unicauca.gasstation.backend.workers.domain.Worker;
import com.edu.unicauca.gasstation.backend.workers.exception.DuplicateDocumentException;
import com.edu.unicauca.gasstation.backend.workers.exception.RoleNotFoundException;
import com.edu.unicauca.gasstation.backend.workers.exception.WorkerNotFoundException;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.WorkerRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for workers (HU-9): create, edit, activate/deactivate, get one and list with filters.
 *
 * <p>Business rules applied here:
 * <ul>
 *   <li>Name and document are stored without leading or trailing spaces.</li>
 *   <li>The document is unique among workers, both when creating and when editing.</li>
 *   <li>The role must exist in the {@code shifts} module.</li>
 *   <li>Every new worker starts active. There is no delete: a worker is deactivated.</li>
 * </ul>
 * Role name and dispenser are always read from {@code shifts} through {@link ShiftExternalService}.
 */
@Service
@RequiredArgsConstructor
public class WorkerService {

    private static final char LIKE_ESCAPE = '\\';

    private final WorkerRepository workerRepository;
    private final ShiftExternalService shiftExternalService;

    /**
     * Registers a new worker, always active.
     *
     * @throws DuplicateDocumentException if another worker already has the document
     * @throws RoleNotFoundException      if the role does not exist
     */
    @Transactional
    public WorkerResponse create(CreateWorkerRequest request) {
        String fullName = request.fullName().strip();
        String document = request.document().strip();

        if (workerRepository.existsByDocument(document)) {
            throw new DuplicateDocumentException(document);
        }
        RoleInfo role = findRole(request.roleId());

        // saveAndFlush sends the INSERT now, so a concurrent duplicate fails inside this call
        Worker worker = workerRepository.saveAndFlush(new Worker(fullName, document, role.id()));
        return toResponse(worker, role);
    }

    /**
     * Replaces name, document and role of a worker. Inactive workers can be edited too.
     *
     * @throws WorkerNotFoundException    if the worker does not exist
     * @throws DuplicateDocumentException if a different worker already has the document
     * @throws RoleNotFoundException      if the role does not exist
     */
    @Transactional
    public WorkerResponse update(UUID id, UpdateWorkerRequest request) {
        Worker worker = findWorker(id);
        String fullName = request.fullName().strip();
        String document = request.document().strip();

        if (workerRepository.existsByDocumentAndIdNot(document, id)) {
            throw new DuplicateDocumentException(document);
        }
        RoleInfo role = findRole(request.roleId());

        worker.updateDetails(fullName, document, role.id());
        // saveAndFlush sends the UPDATE now, so a concurrent duplicate fails inside this call
        return toResponse(workerRepository.saveAndFlush(worker), role);
    }

    /**
     * Activates or deactivates a worker. Setting the state it already has is not an error.
     *
     * @throws WorkerNotFoundException if the worker does not exist
     */
    @Transactional
    public WorkerResponse changeStatus(UUID id, boolean active) {
        Worker worker = findWorker(id);
        if (active) {
            worker.activate();
        } else {
            worker.deactivate();
        }
        return toResponse(workerRepository.save(worker), findRole(worker.getRoleId()));
    }

    /**
     * Returns one worker, active or not.
     *
     * @throws WorkerNotFoundException if the worker does not exist
     */
    @Transactional(readOnly = true)
    public WorkerResponse getById(UUID id) {
        Worker worker = findWorker(id);
        return toResponse(worker, findRole(worker.getRoleId()));
    }

    /**
     * Lists workers ordered by full name. Every filter is optional and they can be combined.
     *
     * @param active only workers with this status; {@code null} for all
     * @param roleId only workers with this role; {@code null} for all
     * @param search text contained in the name or in the document, ignoring case; blank for all
     */
    @Transactional(readOnly = true)
    public List<WorkerResponse> list(Boolean active, UUID roleId, String search) {
        List<Worker> workers = workerRepository.findAll(buildFilter(active, roleId, search), Sort.by("fullName"));

        // A single call to shifts for every role in the list, not one per worker
        Set<UUID> roleIds = workers.stream().map(Worker::getRoleId).collect(Collectors.toSet());
        Map<UUID, RoleInfo> roles = shiftExternalService.getRolesByIds(roleIds);

        return workers.stream()
                .map(worker -> toResponse(worker, roles.get(worker.getRoleId())))
                .toList();
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

    private Specification<Worker> buildFilter(Boolean active, UUID roleId, String search) {
        List<Specification<Worker>> filters = new ArrayList<>();
        if (active != null) {
            filters.add((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        if (roleId != null) {
            filters.add((root, query, cb) -> cb.equal(root.get("roleId"), roleId));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + escapeLike(search.strip().toLowerCase()) + "%";
            filters.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(root.get("document")), pattern, LIKE_ESCAPE)));
        }
        return Specification.allOf(filters);
    }

    /** Makes %, _ and \ match literally instead of acting as LIKE wildcards. */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static WorkerResponse toResponse(Worker worker, RoleInfo role) {
        if (role == null) {
            // Cannot happen while the FK worker.role_id -> role.id exists
            throw new IllegalStateException("Role " + worker.getRoleId() + " of worker " + worker.getId() + " not found");
        }
        return new WorkerResponse(
                worker.getId(),
                worker.getFullName(),
                worker.getDocument(),
                worker.getRoleId(),
                role.name(),
                role.dispenser(),
                worker.isActive());
    }
}
