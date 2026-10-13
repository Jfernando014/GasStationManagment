package com.edu.unicauca.gasstation.backend.sales.api;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import com.edu.unicauca.gasstation.backend.sales.api.dto.CashMovementRequestDto;
import com.edu.unicauca.gasstation.backend.sales.api.dto.SavingsFundMovementRequestDto;
import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovementType;
import com.edu.unicauca.gasstation.backend.sales.domain.services.CashService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SavingsFundService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class SalesMovementAuthorizationTest {

    @Mock
    private CashService cashService;

    @Mock
    private SavingsFundService savingsFundService;

    @InjectMocks
    private CashController cashController;

    @InjectMocks
    private SavingsFundController savingsFundController;

    @Test
    void cashMovementWritesRejectNonAdminAuthority() {
        CashMovementRequestDto request = new CashMovementRequestDto(CashMovementType.DEPOSIT,
                LocalDate.of(2026, 8, 12), new BigDecimal("50000.00"), "Consignación");

        assertThrows(AccessDeniedException.class, () -> cashController.register(request, authenticated("READ_ONLY")));

        verifyNoInteractions(cashService);
    }

    @Test
    void savingsFundWritesRejectNonAdminAuthority() {
        SavingsFundMovementRequestDto request = new SavingsFundMovementRequestDto(
                LocalDate.of(2026, 8, 12), new BigDecimal("50000.00"), "Aporte diario");
        Authentication authentication = authenticated("READ_ONLY");

        assertThrows(AccessDeniedException.class,
                () -> savingsFundController.registerContribution(request, authentication));
        assertThrows(AccessDeniedException.class,
                () -> savingsFundController.registerPayment(request, authentication));

        verifyNoInteractions(savingsFundService);
    }

    private Authentication authenticated(String authority) {
        return new UsernamePasswordAuthenticationToken(
                "reader", "unused", List.of(new SimpleGrantedAuthority(authority)));
    }
}