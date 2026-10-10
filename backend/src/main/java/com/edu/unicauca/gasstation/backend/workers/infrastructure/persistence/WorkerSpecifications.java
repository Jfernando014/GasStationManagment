package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerFilter;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities.WorkerEntity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the JPA query for listing workers with optional filters. Used only by {@code WorkerRepositoryImpl}.
 */
public final class WorkerSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private WorkerSpecifications() {
    }

    /**
     * Workers matching every filter that is set: status, role, and text contained in the name or the
     * document (ignoring case). An empty filter matches every worker.
     */
    public static Specification<WorkerEntity> matching(WorkerFilter filter) {
        List<Specification<WorkerEntity>> specs = new ArrayList<>();
        if (filter.active() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("active"), filter.active()));
        }
        if (filter.roleId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("roleId"), filter.roleId()));
        }
        if (filter.hasSearch()) {
            String pattern = "%" + escapeLike(filter.search().strip().toLowerCase()) + "%";
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(root.get("document")), pattern, LIKE_ESCAPE)));
        }
        return Specification.allOf(specs);
    }

    /** Makes %, _ and \ match literally instead of acting as LIKE wildcards. */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
