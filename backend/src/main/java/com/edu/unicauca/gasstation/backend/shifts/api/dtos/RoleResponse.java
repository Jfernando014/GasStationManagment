package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import java.util.UUID;

public record RoleResponse(UUID id, String name, int dispenser) {
}
