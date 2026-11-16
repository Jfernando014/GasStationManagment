# Control de cambios de base de datos

**Autor:** javiersolanop777  
**Ámbito:** Backend GasStationManagment, módulo Sales.

## 1. Separación entre esquema y datos

- Las migraciones versionadas (`db/migration/V*.sql`) son la única fuente del esquema.
- Los datos operativos reales de agosto de 2026 viven en una migración repetible:
  - `db/seed/R__seed_august_2026_operational_data.sql`
- El seed no crea ni altera tablas; solo inserta datos sobre el esquema existente.
- El seed no se ejecuta en producción ni en el perfil por defecto. Solo se carga con el perfil `dev`.

## 2. Activación

`application-dev.properties`:

```properties
spring.flyway.locations=classpath:db/migration,classpath:db/seed
```

Sin ese perfil, Flyway solo lee `db/migration` y el seed no se ejecuta.

## 3. Idempotencia

El seed:

1. Borra únicamente los rangos de fechas que controla.
2. Hace upsert de catálogos estables:
   - roles,
   - códigos de turno,
   - trabajadores,
   - tanques.
3. Vuelve a insertar las filas operativas del periodo.

Se debe poder ejecutar dos veces sin duplicar jornadas, cierres, lecturas, asignaciones ni gastos.

## 4. Datos personales

- El seed contiene nombres y una cédula real.
- Los documentos faltantes usan marcadores provisionales:
  - `SIN-ASIGNAR-ANA`
  - `SIN-ASIGNAR-CARLOS`
  - `SIN-ASIGNAR-WILTOM`
- Antes de usar el seed fuera de desarrollo, reemplazar los marcadores por documentos reales y revisar el tratamiento de datos personales.

## 5. Qué no hace el seed

- No modifica `sales_parameter`.
- No crea productos ni SKU.
- No carga incentivos, horas extra, inspecciones, competencia ni stock.
- No ejecuta `ReportDataService`.
- No toca la rama S3 ni el frontend.
- No reemplaza la conciliación diaria real del sistema.

## 6. Rollback

Para retirar los datos de agosto de 2026:

```sql
DELETE FROM statistics_shift_sale WHERE journal_date BETWEEN DATE '2026-08-01' AND DATE '2026-08-28';
DELETE FROM statistics_journal_sales WHERE journal_date BETWEEN DATE '2026-08-01' AND DATE '2026-08-28';
DELETE FROM cash_movement WHERE movement_date BETWEEN DATE '2026-07-31' AND DATE '2026-08-28';
DELETE FROM savings_fund_movement WHERE movement_date BETWEEN DATE '2026-07-21' AND DATE '2026-08-28';
DELETE FROM dominus_cash_balance WHERE business_date BETWEEN DATE '2026-08-01' AND DATE '2026-08-28';
DELETE FROM points_colombia_day WHERE business_date BETWEEN DATE '2026-08-01' AND DATE '2026-08-28';
DELETE FROM credibanco_vendor_day WHERE business_date BETWEEN DATE '2026-08-01' AND DATE '2026-08-28';
DELETE FROM credibanco_previous_month WHERE month_start = DATE '2026-08-01';
DELETE FROM vendor_shift_closes WHERE journal_date BETWEEN DATE '2026-08-01' AND DATE '2026-08-28';
DELETE FROM administrative_expense WHERE expense_date BETWEEN DATE '2026-08-01' AND DATE '2026-08-28';
DELETE FROM fuel_reception WHERE reception_date BETWEEN DATE '2026-08-01' AND DATE '2026-08-28';
DELETE FROM tank_measurement WHERE journal_date BETWEEN DATE '2026-07-31' AND DATE '2026-08-28';
DELETE FROM shift_assignment WHERE work_date BETWEEN DATE '2026-08-01' AND DATE '2026-08-28';
DELETE FROM journals WHERE journal_date BETWEEN DATE '2026-08-01' AND DATE '2026-08-28';
```

No borrar roles, trabajadores, códigos de turno ni tanques sin revisar sus dependencias.

## 7. Validación

- `AugustSeedMigrationTest` aplica las migraciones y el seed en Testcontainers.
- El test verifica conteos, totales, caja de los días 01 y 02 y repetición del seed.
- `JournalHistoryServiceTest` verifica que el historial exponga la caja real.
- Validación local con PGlite: migraciones + seed dos veces, sin duplicar datos.
- `AugustSeedMigrationTest` pasó con Testcontainers/PostgreSQL 16; suite completa con 1.553 pruebas sin fallos.
- CI solo ejecuta pruebas en PR hacia `main`.

## 8. Regeneración

El generador de apoyo está en:

```text
/tmp/opencode/seedgen/generate_august_seed.py
```

No forma parte del runtime. Se ejecuta solo para regenerar el SQL a partir de los Excel fuente.

---

## 9. Criterio de aceptación

- `mvn test` verde en un entorno con Docker.
- El seed se aplica sobre una base vacía y sobre una base que ya lo contiene.
- Las cifras del día 01 y del cierre mensual coinciden con los documentos fuente.
- Las decisiones estimadas quedan documentadas en `docs/august-2026-seed.md`.
