package com.edu.unicauca.gasstation.backend.shifts.spi;

import java.util.Collection;
import java.util.List;
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

    /**
     * Every active worker, in a single query. The monthly rotation needs it to include titulares that have no row in
     * the grid yet.
     */
    List<ScheduleWorkerInfo> getActiveWorkers();
}
