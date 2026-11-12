package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.workers.domain.models.DayHoursSegment;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerDayHours;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.WorkerDayHoursRepository;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities.DayHoursSegmentEmbeddable;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities.WorkerDayHoursEntity;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.repositories.JpaWorkerDayHoursRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA implementation of {@link WorkerDayHoursRepository}. */
@Repository
@RequiredArgsConstructor
public class WorkerDayHoursRepositoryImpl implements WorkerDayHoursRepository {

    private final JpaWorkerDayHoursRepository repository;

    @Override
    @Transactional
    public void replaceDay(LocalDate workDate, List<WorkerDayHours> records) {
        repository.deleteByWorkDate(workDate);
        repository.saveAllAndFlush(records.stream().map(WorkerDayHoursRepositoryImpl::toEntity).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkerDayHours> findByDate(LocalDate workDate) {
        return repository.findByWorkDateOrderByWorkerNameAscWorkerIdAsc(workDate).stream()
                .map(WorkerDayHoursRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkerDayHours> findByWorkerBetween(UUID workerId, LocalDate from, LocalDate to) {
        return repository.findByWorkerIdAndWorkDateBetweenOrderByWorkDateAsc(workerId, from, to).stream()
                .map(WorkerDayHoursRepositoryImpl::toDomain)
                .toList();
    }

    private static WorkerDayHours toDomain(WorkerDayHoursEntity entity) {
        List<DayHoursSegment> segments = entity.getSegments().stream()
                .map(segment -> new DayHoursSegment(segment.getStartHour(), segment.getEndHour(), segment.getHours()))
                .toList();
        return new WorkerDayHours(entity.getWorkDate(), entity.getWorkerId(), entity.getWorkerName(),
                entity.getWorkedCode(), entity.getGridCode(), entity.getDayRole(), entity.getTotalHours(),
                entity.getNoveltyType(), entity.getReplacedWorkerId(), entity.getReplacedWorkerName(),
                entity.getReason(), entity.getJournalId(), entity.isNoveltiesApplied(), segments);
    }

    private static WorkerDayHoursEntity toEntity(WorkerDayHours record) {
        WorkerDayHoursEntity entity = new WorkerDayHoursEntity();
        entity.setWorkDate(record.workDate());
        entity.setWorkerId(record.workerId());
        entity.setWorkerName(record.workerName());
        entity.setWorkedCode(record.workedCode());
        entity.setGridCode(record.gridCode());
        entity.setDayRole(record.dayRole());
        entity.setTotalHours(record.totalHours());
        entity.setNoveltyType(record.noveltyType());
        entity.setReplacedWorkerId(record.replacedWorkerId());
        entity.setReplacedWorkerName(record.replacedWorkerName());
        entity.setReason(record.reason());
        entity.setJournalId(record.journalId());
        entity.setNoveltiesApplied(record.noveltiesApplied());
        record.segments().forEach(segment -> entity.getSegments()
                .add(new DayHoursSegmentEmbeddable(segment.startHour(), segment.endHour(), segment.hours())));
        return entity;
    }
}
