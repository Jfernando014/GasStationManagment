package com.edu.unicauca.gasstation.backend.shifts;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Only entry point other modules may use to query the shift catalog and roles.
 * Returns only UUIDs and the public records of this package, never internal classes.
 */
public interface ShiftExternalService {

    Optional<UUID> findShiftCodeIdByCode(String code);

    List<ShiftCodeInfo> getActiveCatalog();

    /**
     * Looks up several roles in a single query.
     *
     * @param roleIds ids to look up; duplicates are ignored
     * @return roles found, keyed by id. An id missing from the map means that role does not exist
     */
    Map<UUID, RoleInfo> getRolesByIds(Collection<UUID> roleIds);
}
