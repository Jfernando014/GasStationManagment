package com.edu.unicauca.gasstation.backend.appearance.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.appearance.api.dtos.ColorSettingDtos.ColorUpdateItemRequest;
import com.edu.unicauca.gasstation.backend.appearance.api.dtos.ColorSettingDtos.UpdateColorThemeRequest;
import com.edu.unicauca.gasstation.backend.appearance.api.dtos.ColorSettingDtos.UpdateSingleColorRequest;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorCatalog;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorCategory;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorTheme;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorUpdateCommand;
import com.edu.unicauca.gasstation.backend.appearance.domain.services.ColorSettingService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class ColorSettingControllerTest {

    private ColorSettingService service;
    private ColorSettingController controller;

    @BeforeEach
    void setUp() {
        service = mock(ColorSettingService.class);
        controller = new ColorSettingController(service);
    }

    @Test
    void returnsThemeForAnyAuthenticatedRequest() {
        ColorTheme expected = new ColorTheme(List.of(), List.of(), Map.of(), ColorCatalog.curatedPalette());
        when(service.getTheme()).thenReturn(expected);

        var response = controller.getTheme();

        assertThat(response.palette()).hasSize(16);
        verify(service).getTheme();
    }

    @Test
    void rejectsNonAdminWritesAndResets() {
        Authentication seller = new UsernamePasswordAuthenticationToken(
                "seller", "n/a", List.of(new SimpleGrantedAuthority("SELLER")));

        assertThatThrownBy(() -> controller.updateSingle(
                ColorCategory.FUEL, "MOTOR", new UpdateSingleColorRequest("#2563EB"), seller))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.resetAll(null, null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        verifyNoInteractions(service);
    }

    @Test
    void delegatesBatchUpdateForAdmin() {
        Authentication admin = new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ADMIN")));
        ColorTheme expected = new ColorTheme(List.of(), List.of(), Map.of(), ColorCatalog.curatedPalette());
        List<ColorUpdateCommand> expectedCommands = List.of(
                new ColorUpdateCommand(ColorCategory.FUEL, "MOTOR", "#2563EB"));
        when(service.updateBatch(expectedCommands)).thenReturn(expected);

        var response = controller.updateBatch(
                new UpdateColorThemeRequest(List.of(
                        new ColorUpdateItemRequest(ColorCategory.FUEL, "MOTOR", "#2563EB"))),
                admin);

        assertThat(response).isNotNull();
        verify(service).updateBatch(expectedCommands);
    }
}
