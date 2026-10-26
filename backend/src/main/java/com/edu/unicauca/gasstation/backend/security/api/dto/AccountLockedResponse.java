package com.edu.unicauca.gasstation.backend.security.api.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLockedResponse {
    private String error;
    private String message;
    private long minutesRemaining;
    private LocalDateTime lockedUntil;
}
