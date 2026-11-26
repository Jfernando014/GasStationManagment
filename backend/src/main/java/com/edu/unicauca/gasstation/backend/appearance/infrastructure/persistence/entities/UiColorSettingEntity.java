package com.edu.unicauca.gasstation.backend.appearance.infrastructure.persistence.entities;

import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/** JPA mapping of the {@code ui_color_setting} table (migration V48). */
@Entity
@Table(name = "ui_color_setting", uniqueConstraints = @UniqueConstraint(
        name = "uk_ui_color_setting_category_key", columnNames = {"category", "entity_key"}))
@Getter
@Setter
@NoArgsConstructor
public class UiColorSettingEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ColorCategory category;

    @Column(name = "entity_key", nullable = false, length = 64)
    private String entityKey;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
