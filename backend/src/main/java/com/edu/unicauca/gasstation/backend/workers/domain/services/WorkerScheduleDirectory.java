package com.edu.unicauca.gasstation.backend.workers.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.ScheduleWorkerDirectory;
import com.edu.unicauca.gasstation.backend.shifts.ScheduleWorkerInfo;
import com.edu.unicauca.gasstation.backend.workers.domain.models.Worker;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.WorkerRepository;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the {@link ScheduleWorkerDirectory} declared by {@code shifts}, so the shift schedule can read
 * workers without {@code shifts} depending on this module. Only converts {@link Worker}; no rules of its own.
 */
@Service
@RequiredArgsConstructor
public class WorkerScheduleDirectory implements ScheduleWorkerDirectory {

    private final WorkerRepository workerRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ScheduleWorkerInfo> getWorkersByIds(Collection<UUID> workerIds) {
        if (workerIds.isEmpty()) {
            return Map.of();
        }
        return workerRepository.findAllById(workerIds).stream()
                .map(WorkerScheduleDirectory::toInfo)
                .collect(Collectors.toMap(ScheduleWorkerInfo::id, info -> info));
    }

    private static ScheduleWorkerInfo toInfo(Worker worker) {
        return new ScheduleWorkerInfo(worker.getId(), worker.getFullName(), worker.getRoleId(), worker.isActive());
    }
}
