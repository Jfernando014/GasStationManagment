package com.edu.unicauca.gasstation.backend.sales.domain.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reusable input DTO for the Dominus supplement. Every monetary value is supplied by
 * the caller, including totals. Null cells mean blank; zero and negative values are preserved.
 */
public record DominusCashComplement(
        @NotNull @Valid Header header,
        @NotNull @Valid LubricantSales lubricantSales,
        @NotNull @Valid CreditSales creditSales,
        @NotNull @Valid Expenses expenses,
        @NotNull @Valid Deposits deposits,
        @NotNull @Valid DailyBalance dailyBalance,
        @NotNull @Valid AccumulatedCash accumulatedCash) {

    public record Header(
            @NotNull LocalDate reportDate,
            @NotBlank @Size(max = 30) String stationCode,
            @NotBlank @Size(max = 80) String stationName,
            @Size(max = 80) String administrator) {
    }

    /** The middle column may contain a detail OR an intermediate amount, never a derived value. */
    public record Values(
            @Size(max = 80) String detail,
            @Digits(integer = 12, fraction = 2) BigDecimal intermediateAmount,
            @Digits(integer = 12, fraction = 2) BigDecimal amount) {
        @JsonIgnore
        @AssertTrue(message = "Use either detail or intermediateAmount for the middle column")
        public boolean isMiddleColumnUnambiguous() {
            return intermediateAmount == null || detail == null || detail.isBlank();
        }
    }

    public record Row(@Size(max = 80) String concept, @NotNull @Valid Values values) {
    }

    public record LubricantSales(
            @NotNull @Valid Values oils,
            @NotNull @Valid Values additivesWaterLiquids,
            @NotNull @Valid Values miscellaneousGoods,
            @NotNull @Size(max = 20) List<@NotNull @Valid Row> additionalRows,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal total) {
    }

    public record CreditSales(
            @NotNull @Valid Values sotracauca,
            @NotNull @Valid Values puntosColombia,
            @NotNull @Size(max = 20) List<@NotNull @Valid Row> additionalRows,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal total) {
    }

    public record Expenses(
            @NotNull @Size(max = 20) List<@NotNull @Valid Row> rows,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal total) {
    }

    public record Deposits(
            @NotNull @Valid Values credibanco,
            @NotNull @Valid Values bancolombiaPrimax,
            @NotNull @Valid Values qr,
            @NotNull @Size(max = 20) List<@NotNull @Valid Row> additionalRows,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal total) {
    }

    public record DailyBalance(
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal dailySales,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal expenses,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal credibancos,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal credits,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal qr,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal dailyCash) {
    }

    public record AccumulatedCash(
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal previousCash,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal dailyCash,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal deposit,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal accumulatedBalance) {
    }
}
