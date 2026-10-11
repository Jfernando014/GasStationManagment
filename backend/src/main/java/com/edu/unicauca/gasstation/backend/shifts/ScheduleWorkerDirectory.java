package com.edu.unicauca.gasstation.backend.shifts;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Gives the shift schedule the data it needs about workers.
 *
 * <p>Declared here and implemented by the {@code workers} module, so {@code shifts} never depends on
 * {@code workers} (which already depends on {@code shifts}); a dependency in both directions would be a cycle.
 */
public interface ScheduleWorkerDirectory {

    /**
     * Looks up several workers in a single query.
     *
     * @param workerIds ids to look up; duplicates are ignored
     * @return workers found, keyed by id. An id missing from the map means that worker does not exist
     */
    Map<UUID, ScheduleWorkerInfo> getWorkersByIds(Collection<UUID> workerIds);
}
