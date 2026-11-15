# Seed operativo de agosto de 2026

**Autor:** javiersolanop777  
**EDS:** Primax Bellavista  
**Periodo:** 01/08/2026 a 28/08/2026  
**Archivo:** `db/seed/R__seed_august_2026_operational_data.sql`

## 1. Objetivo

Cargar los datos reales de operación de agosto de 2026 para que el historial de jornadas, la caja, el inventario y las estadísticas tengan información coherente con los Excel de la estación.

## 2. Fuentes

| Archivo | Contenido usado |
|---|---|
| `Control Juan Clavijo Agosto 2026.xlsx` | Hojas diarias 01–29, `Registro`, `Inventario`, horarios. |
| `4 SALDO EN CAJA BELLAVISTA AGOSTO 2026.xlsx` | Planilla, saldo Dominus, diferencia, ahorro. |
| `5 REPORTE CREDIBANCO BELLAVISTA AGOSTO 2026(1).xlsx` | Mastercard, Visa, Amex, QR. |
| `8 PUNTOS COLOMBIA BELLAVISTA AGOSTO 2026.xlsx` | Puntos Colombia por día. |
| `9 GASTOS ADMINISTRATIVOS BELLAVISTA AGOSTO 2026.xlsx` | Gastos administrativos. |
| `TABLA AFORO.xlsx` | Capacidades de tanques. |
| `7 VARIACIÓN BELLAVISTA AGOSTO 2026.xlsx` | Solo referencia; no se usa para el físico. |

## 3. Cobertura generada

| Elemento | Cantidad |
|---|---:|
| Jornadas | 28 |
| Pasos de jornada | 168 |
| Cierres por vendedor | 78 |
| Asignaciones de turno | 112 |
| Mediciones de tanque | 23 |
| Lecturas de tanque | 138 |
| Recepciones de combustible | 11 filas en 7 días |
| Gastos administrativos | 12 |
| Movimientos de caja | 18 |
| Movimientos de ahorro | 40 (39 aportes + 1 pago) |
| Snapshots de jornada | 28 |
| Snapshots por cierre | 78 |

## 4. Reglas aplicadas

### 4.1 Jornadas

- Consecutivos 289 a 316.
- Estado `CLOSED`.
- Seis pasos `COMPLETED`.
- Los títulos de los pasos 1–5 se toman de `JournalClosingSummaryService.STEP_TITLES`.
- El paso 6 es `Cierre de jornada`.

### 4.2 Fechas

- La hoja `DD` de `Control` contiene las ventas del día `DD`.
- La etiqueta `Medidas tanques DD` del `Registro` cierra el día anterior:
  - `tank_measurement.journal_date = fecha_etiqueta - 1 día`.
- No se siembra el cierre del 29/08.

### 4.3 Cierres

```text
total_delivered = cash + ga + credibanco + qr + redemptions
real_discrepancy = total_delivered - dominus_tirilla
```

- Estado `CUADRA`, `SOBRA` o `FALTA`.
- El “Descuadre real” del Excel viene con el signo invertido; se normaliza en el seed.

Día 01 confirmado:

- Ana: `+1.477,48`
- Carlos: `+1.758,93`
- Total: `+3.236,41`

### 4.4 Caja

```text
planilla = cash_movements_balance + accumulated_station_cash
difference = dominus_accumulated - planilla
```

- Saldo inicial: `12.114.000` el `2026-07-31`.
- Día 01 no tiene movimiento de caja: `12.114.000 + 12.516.000 = 24.630.000`.
- Día 02: depósito `29.616.000`.
- Día 03: depósito `13.699.650`.
- Ajustes negativos: día 06 `46.500`, día 14 `20.000`, día 18 `20.750`, día 20 `10.500`.
- Diferencia día 01: `-531`.
- Diferencia día 02: `-964`.

### 4.5 Ahorro

- 39 aportes diarios de `250.000` entre `21/07/2026` y `28/08/2026`.
- Pago de `7.750.000` el `23/08/2026`.
- Saldo neto al 28/08: `2.000.000`.
- La hoja `4 SALDO` deja celdas vacías algunos días; el seed mantiene los aportes diarios según la regla acordada.

### 4.6 Gastos

- 12 filas; total `187.950`.
- El monto está en `VALOR` y el NIT en `N`.
- La descripción combina razón social y concepto.

### 4.7 Tarjetas, QR y puntos

- El reporte Credibanco solo trae totales diarios por franquicia.
- Mastercard y Visa se distribuyen por vendedor usando su `credibanco` real de `Control`.
- El redondeo se ajusta en el último vendedor del día.
- QR y redenciones son valores reales por vendedor.
- Puntos: `1.796.955`; el día 01 suma `46.050` en `8 PUNTOS` y `46.055` en `Control`; la diferencia de `5` queda documentada.
- El reporte Credibanco del 27/08 trae en QR `130.640`, que corresponde a las redenciones de Ana en `Control`; el QR real de ese día es `632.655`. Diferencia: `502.015`.
- `credibanco_vendor_day.planilla_qr` conserva el QR real de `Control`; `credibanco_previous_month.qr` es `21.926.590`.

Totales mensuales:

| Concepto | Valor |
|---|---:|
| Mastercard | 66.683.965 |
| Visa | 32.184.828 |
| Amex | 0 |
| Total EDS | 98.868.793 |
| QR según reporte Credibanco | 19.476.459 |
| QR real por vendedor (Control) | 19.978.474 |
| Total con QR (reporte) | 118.345.252 |
| Total con QR (Control) | 118.847.267 |
| Puntos | 1.796.955 |
| Comisión 2,65% | 2.620.023,01 |

### 4.8 Inventario

| Código | Combustible | Capacidad a 230 cm |
|---|---|---:|
| T1C1 | Motor | 3.090 |
| T1C2 | Motor | 3.059 |
| T2C1 | Motor | 3.061 |
| T2C2 | Diesel | 3.065 |
| T3 | Maxpro | 1.941 |
| T4 | Extra | 1.936 |

- 23 mediciones y 138 lecturas.
- Fechas: 31/07 y las etiquetas 01, 03, 04, 05, 06, 08, 10, 11, 12, 13, 14, 15, 18, 19, 20, 21, 22, 24, 25, 26, 27, 28 y 29.
- No se inventan los días 02, 07, 09, 16, 17, 23, 30 ni 31.

### 4.9 Recepciones

Días: 01, 05, 11, 14, 19, 21 y 28/08.

Hay 11 filas porque algunos días tienen más de un producto:

- 01/08: Motor 3.020 y Maxpro 600.
- 05/08: Motor 3.970.
- 11/08: Motor 3.020 y Diesel 950.
- 14/08: Motor 3.020 y Extra 950.
- 19/08: Motor 3.970.
- 21/08: Motor 3.970.
- 28/08: Motor 3.020 y Diesel 950.

El proveedor no viene en la fuente; se usa `SIN DATO`.

### 4.10 Grilla de turnos

- 01–02/08: `Copia de Copia de HORARIO AGOST`.
- 03–28/08: `HORARIO AGOSTO`.
- `46215` → `6-9y5-9`.
- `46182` → `DIA`.
- Vacío → `DESCANSO`.
- Si hay cierre y el código es `DESCANSO` o vacío:
  - Mónica → `6-9y5-9`.
  - Resto → `DIA`.
- Resultado: 112 filas con `origin = 'MANUAL'`.

### 4.11 Trabajadores

| Persona | Documento | Rol |
|---|---|---|
| Ana Milena Villamil | `SIN-ASIGNAR-ANA` | TITULAR |
| Carlos Andrés Paredes | `SIN-ASIGNAR-CARLOS` | TITULAR |
| Wiltom Andrés Paz | `SIN-ASIGNAR-WILTOM` | TITULAR |
| Mónica Dorado | `52875796` | APOYO |

Los documentos `SIN-ASIGNAR-*` son provisionales.

## 5. Estadísticas

- `statistics_journal_sales`: 28 filas `PROCESSED`.
- `statistics_shift_sale`: 78 filas.
- Galones con escala 3.
- Valores monetarios con escala 2.
- `worked_code` es el código de turno.
- `effective_role` corresponde al rol del trabajador.

## 6. Historial de caja

`JournalHistoryService` ahora consulta `CashBalanceQuery` y publica:

- `planilla`: consecutivo de la jornada.
- `previousCash`: `planillaCashBalance`; si no existe, `cashBalance`.
- `accumulatedSavings`: saldo acumulado del fondo.
- `dominusBalance`: saldo acumulado de Dominus.

## 7. Verificación

- `AugustSeedMigrationTest` aplica migraciones y seed con Testcontainers.
- `JournalHistoryServiceTest` cubre la caja real y el fallback.
- El seed debe ejecutarse dos veces sin duplicar datos.
- Validación local con PGlite: se aplicaron todas las migraciones y el seed dos veces; pasaron conteos, totales, inventario y caja.
- `AugustSeedMigrationTest` pasó contra PostgreSQL 16 con Testcontainers.
- Suite completa `mvn test`: 1.553 pruebas, 0 fallos, 0 errores.

## 8. Gaps y decisiones pendientes

- Documentos reales de Ana, Carlos y Wiltom.
- Confirmar si los depósitos/ajustes calculados por diferencia de planilla deben registrarse también como gastos o movimientos separados.
- Confirmar el tratamiento de la diferencia de 5 puntos del día 01.
- Revisar con el negocio la distribución estimada de Mastercard/Visa por vendedor.
- Confirmar el QR del 27/08: `Control` registra `632.655` y el reporte Credibanco `130.640`.
- Confirmar el depósito calculado del día 15 (`136.100`).
