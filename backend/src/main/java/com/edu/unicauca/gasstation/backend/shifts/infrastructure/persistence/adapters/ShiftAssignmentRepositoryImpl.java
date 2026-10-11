package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftAssignmentRepository;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.ShiftAssignmentEntity;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.mappers.ShiftAssignmentPersistenceMapper;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.repositories.JpaShiftAssignmentRepository;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.repositories.JpaShiftCodeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the domain {@link ShiftAssignmentRepository} with Spring Data JPA.
 */
@Repository
@RequiredArgsConstructor
public class ShiftAssignmentRepositoryImpl implements ShiftAssignmentRepository {

    private final JpaShiftAssignmentRepository repository;
    private final JpaShiftCodeRepository shiftCodeRepository;
    private final ShiftAssignmentPersistenceMapper mapper;

    @Override
    @Transactional
    public ShiftAssignment save(ShiftAssignment assignment) {
        return withSavedId(assignment, repository.save(toEntity(assignment)));
    }

    @Override
    @Transactional
    public List<ShiftAssignment> saveAll(List<ShiftAssignment> assignments) {
        List<ShiftAssignmentEntity> saved = repository.saveAll(assignments.stream().map(this::toEntity).toList());
        return IntStream.range(0, assignments.size())
                .mapToObj(i -> withSavedId(assignments.get(i), saved.get(i)))
                .toList();
    }

    @Override
    public List<ShiftAssignment> findByWorkDateBetween(LocalDate from, LocalDate to) {
        return repository.findByWorkDateBetween(from, to).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<ShiftAssignment> findByWorkerIdAndWorkDate(UUID workerId, LocalDate workDate) {
        return repository.findByWorkerIdAndWorkDate(workerId, workDate).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public int deleteByWorkerInRange(UUID workerId, LocalDate from, LocalDate to) {
        return repository.deleteByWorkerInRange(workerId, from, to);
    }

    /** A reference (proxy) to the shift code is enough to write shift_code_id; the code row is not modified. */
    private ShiftAssignmentEntity toEntity(ShiftAssignment assignment) {
        return mapper.toEntity(assignment, shiftCodeRepository.getReferenceById(assignment.getShiftCode().getId()));
    }

    /**
     * The saved assignment with the id given by the database. Built from the domain object that was saved, so the
     * shift code reference does not have to be loaded again.
     */
    private static ShiftAssignment withSavedId(ShiftAssignment assignment, ShiftAssignmentEntity saved) {
        return ShiftAssignment.restore(saved.getId(), assignment.getWorkerId(), assignment.getWorkDate(),
                assignment.getShiftCode(), assignment.getNote());
    }
}
