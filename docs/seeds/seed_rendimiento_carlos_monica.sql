-- Seed de rendimiento: Carlos y Mónica frente al mes y frente a todo el historial.
--
-- Qué agrega (del 29/08/2026 al 31/10/2026, de modo que el historial sea continuo desde el 01/08/2026 y
-- las ventanas "mes de agosto", "septiembre", "octubre" y "agosto a octubre" tengan todos sus días procesados):
--   1. Historial de ventas por turno (statistics_journal_sales y statistics_shift_sale) de Carlos y Mónica.
--      Ana y Wiltom también llevan turnos: sin ellos Carlos y Mónica solo se compararían consigo mismos
--      porque la comparación agrupa por día de la semana, período, tipo de día, código de turno y rol.
--   2. La malla de esos días (shift_assignment), con el ciclo de 6 días de los titulares y a Mónica en apoyo.
--   3. El perfil de distribución del libro de objetivos (solo si no hay ninguno) y las metas mensuales de
--      agosto, septiembre y octubre (solo las que no existan), para ver el cumplimiento frente al mes.
--
-- Es idempotente y no pisa datos: una fecha que ya tiene jornada procesada en statistics_journal_sales se omite, una
-- asignación existente se conserva y una meta o un perfil existentes no se tocan. Los valores salen de los promedios
-- por escenario del libro "6 OBJETIVOS VENDEDORES BELLAVISTA AGOSTO 2026" y de los cierres reales de agosto, con una
-- variación determinista por día y una tendencia por mes (Carlos mejora hacia octubre).
--
-- Uso:  docker exec -i gas-station-db-dev psql -U postgres_admin -d gasstation_db -v ON_ERROR_STOP=1 < docs/seeds/seed_rendimiento_carlos_monica.sql
-- Para repetirlo con otros valores, borra antes el rango:
--   DELETE FROM statistics_shift_sale   WHERE journal_date BETWEEN DATE '2026-08-29' AND DATE '2026-10-31';
--   DELETE FROM statistics_journal_sales WHERE journal_date BETWEEN DATE '2026-08-29' AND DATE '2026-10-31';

BEGIN;

-- Fechas que se agregan: las que todavía no tienen jornada procesada.
CREATE TEMP TABLE seed_days ON COMMIT DROP AS
SELECT d::date AS day, (d::date - DATE '2026-08-01') AS n
FROM generate_series(DATE '2026-08-29', DATE '2026-10-31', INTERVAL '1 day') AS d
WHERE NOT EXISTS (SELECT 1 FROM statistics_journal_sales s WHERE s.journal_date = d::date);

-- Vendedores: ciclo de turnos (6 días para titulares, apoyo de lunes a sábado) y factor de rendimiento por mes.
CREATE TEMP TABLE seed_workers ON COMMIT DROP AS
SELECT * FROM (VALUES
    ('SIN-ASIGNAR-ANA',    'Ana Milena Villamil',   'TITULAR', ARRAY['DIA', 'NOCHE', 'NOCHE', 'DESCANSO', 'DESCANSO', 'DIA6'],  1, 1.04, 1.02, 1.00),
    ('SIN-ASIGNAR-CARLOS', 'Carlos Andrés Paredes', 'TITULAR', ARRAY['NOCHE', 'DESCANSO', 'DESCANSO', 'DIA', 'DIA6', 'NOCHE'],  2, 0.96, 1.00, 1.07),
    ('SIN-ASIGNAR-WILTOM', 'Wiltom Andrés Paz',     'TITULAR', ARRAY['DESCANSO', 'DIA', 'DIA', 'NOCHE', 'NOCHE', 'DESCANSO'],  3, 0.96, 0.95, 0.97),
    ('52875796',           'Mónica Dorado',         'APOYO',   NULL::text[],                                                    4, 1.00, 1.05, 0.98)
) AS t(document, full_name, role, cycle, k, factor_aug, factor_sep, factor_oct);

-- Un código por vendedor y día.
CREATE TEMP TABLE seed_plan ON COMMIT DROP AS
SELECT w.document, w.full_name, w.role, w.k, d.day, d.n,
       CASE WHEN w.role = 'APOYO' THEN CASE WHEN d.n % 7 = 6 THEN 'DESCANSO' ELSE '6-9y5-9' END
            ELSE w.cycle[(d.n % 6) + 1] END AS code,
       CASE EXTRACT(MONTH FROM d.day) WHEN 8 THEN w.factor_aug WHEN 9 THEN w.factor_sep ELSE w.factor_oct END AS factor
FROM seed_workers w CROSS JOIN seed_days d;

-- 1. Malla de los días agregados (no pisa una asignación existente).
INSERT INTO shift_assignment (worker_id, work_date, shift_code_id, origin)
SELECT w.id, p.day, sc.id, 'MANUAL'
FROM seed_plan p
JOIN worker w ON w.document = p.document
JOIN shift_code sc ON sc.code = p.code
ON CONFLICT (worker_id, work_date) DO NOTHING;

-- 2. Cierres por turno. Galones totales = promedio del escenario (día de la semana y período) x factor del mes x variación.
CREATE TEMP TABLE seed_sales ON COMMIT DROP AS
WITH base AS (
    SELECT p.*,
           (abs(sin(p.n * 12.9898 + p.k * 78.233) * 43758.5453)::numeric % 1) AS u1,
           (abs(sin(p.n * 39.3468 + p.k * 11.135) * 24634.6345)::numeric % 1) AS u2,
           (abs(sin(p.n * 73.1560 + p.k * 45.164) * 15731.7430)::numeric % 1) AS u3,
           CASE
               WHEN p.code = 'NOCHE' THEN (ARRAY[275.8, 263.0, 289.4, 306.3, 302.3, 299.4, 206.4])[EXTRACT(ISODOW FROM p.day)::int]
               WHEN p.role = 'APOYO' THEN (ARRAY[136.1, 163.3, 186.2, 204.3, 173.9, 177.8, 238.2])[EXTRACT(ISODOW FROM p.day)::int]
               ELSE (ARRAY[449.6, 422.5, 440.3, 443.8, 486.5, 569.0, 428.5])[EXTRACT(ISODOW FROM p.day)::int]
           END
           * CASE WHEN p.code = 'DIA6' THEN 0.55 ELSE 1 END
           -- 12/10/2026 es festivo: el libro registra menos venta (festivo-dia 407, festivo-noche 194)
           * CASE WHEN p.day = DATE '2026-10-12' THEN CASE WHEN p.code = 'NOCHE' THEN 0.72 ELSE 0.90 END ELSE 1 END
           * p.factor AS expected
    FROM seed_plan p
    WHERE p.code <> 'DESCANSO'
), gallons AS (
    SELECT b.*, b.expected * (0.88 + 0.24 * b.u1) AS total_gallons
    FROM base b
), split AS (
    SELECT g.*,
           round(g.total_gallons * CASE WHEN g.role = 'APOYO' THEN 0.95 ELSE 0.85 END, 3) AS motor,
           round(g.total_gallons * CASE WHEN g.role = 'APOYO' THEN 0.03 ELSE 0.07 END, 3) AS diesel,
           round(g.total_gallons * CASE WHEN g.role = 'APOYO' THEN 0.02 ELSE 0.05 END, 3) AS extra,
           round(g.total_gallons * CASE WHEN g.role = 'APOYO' THEN 0.00 ELSE 0.03 END, 3) AS maxpro,
           -- canasta por galón vendido, como en agosto: dia ~320, noche ~200, apoyo ~436
           round(g.total_gallons * CASE WHEN g.code = 'NOCHE' THEN 200 WHEN g.role = 'APOYO' THEN 436 ELSE 320 END
                 * (0.70 + 0.60 * g.u2) / 100) * 100 AS basket
    FROM gallons g
)
SELECT s.*,
       round(s.motor * 16330 + s.diesel * 11930 + s.extra * 20390 + s.maxpro * 12790 + s.basket, 2) AS tirilla
FROM split s;

-- 3. Jornadas procesadas (una por fecha agregada).
INSERT INTO statistics_journal_sales (journal_date, journal_id, consecutive, closed_at, closed_at_nano, status, attempts,
                                      sale_count, processed_at)
SELECT d.day, NULL, 900000 + d.n, (d.day + 1)::timestamp + TIME '06:00:00', 0, 'PROCESSED', 1,
       (SELECT count(*) FROM seed_sales s WHERE s.day = d.day)::int,
       (d.day + 1)::timestamp + TIME '06:05:00'
FROM seed_days d;

INSERT INTO statistics_shift_sale (journal_date, vendor_document, shift_name, worker_id, vendor_name, worked_code,
    effective_role, support, motor_gallons, motor_gallons_scale, diesel_gallons, diesel_gallons_scale, extra_gallons,
    extra_gallons_scale, maxpro_gallons, maxpro_gallons_scale, basket_dominus_amount_cop, basket_dominus_amount_cop_scale,
    dominus_tirilla_cop, dominus_tirilla_cop_scale, total_delivered_cop, total_delivered_cop_scale)
SELECT s.day, s.document, s.code, w.id, s.full_name, s.code, s.role, s.role = 'APOYO',
       s.motor, 3, s.diesel, 3, s.extra, 3, s.maxpro, 3, s.basket, 2, s.tirilla, 2,
       round(s.tirilla + (s.u3 - 0.5) * 3000), 2
FROM seed_sales s
JOIN worker w ON w.document = s.document;

-- 4. Perfil de distribución del libro (hoja PARAMETROS): solo si todavía no hay ninguno.
WITH profile AS (
    INSERT INTO sales_distribution_profiles (name, source, titular_share, support_share, reference_titular_share,
                                             reference_support_share)
    SELECT 'Reparto oficial 70/30 agosto 2026',
           'Libro 6 OBJETIVOS VENDEDORES BELLAVISTA AGOSTO 2026, hoja PARAMETROS (curva horaria, promedio por día, 70/30)',
           0.7, 0.3, 0.5, 0.5
    WHERE NOT EXISTS (SELECT 1 FROM sales_distribution_profiles)
    RETURNING id
), hours AS (
    INSERT INTO sales_distribution_hour_weights
    SELECT profile.id, w.hour - 1, w.weight
    FROM profile,
         unnest(ARRAY[1.1, 0.9, 0.5, 0.4, 0.5, 1.85, 5.3, 6.6, 7.0, 8.1, 8.05, 9.0, 9.3, 9.6, 9.15, 9.6, 9.2, 9.85,
                      13.3, 12.7, 10.9, 8.2, 4.9, 2.1]::numeric[]) WITH ORDINALITY AS w(weight, hour)
    RETURNING 1
), days AS (
    INSERT INTO sales_distribution_day_baselines
    SELECT profile.id, d.weekday, d.gallons
    FROM profile,
         (VALUES (1, 701.54), (2, 705.34), (3, 764.49), (4, 756.2), (5, 799.65), (6, 883.42), (7, 625.86))
             AS d(weekday, gallons)
    RETURNING 1
)
INSERT INTO sales_distribution_support_codes
SELECT profile.id, c.code
FROM profile, (VALUES ('12-7'), ('6-9y5-9'), ('2-9pm'), ('6-9am'), ('2-6pm'), ('6-9y2-6')) AS c(code);

-- 5. Metas mensuales (las que ya existan se conservan). Agosto sale de CALC METAS: 26.920 galones y 8.400.000 pesos.
INSERT INTO sales_targets (target_year, target_month, category, target_value, unit) VALUES
    (2026, 8,  'FUEL_AGGREGATE', 26920,   'GALLONS'),
    (2026, 8,  'BASKET',         8400000, 'COP'),
    (2026, 9,  'FUEL_AGGREGATE', 27000,   'GALLONS'),
    (2026, 9,  'BASKET',         8500000, 'COP'),
    (2026, 10, 'FUEL_AGGREGATE', 27600,   'GALLONS'),
    (2026, 10, 'BASKET',         8700000, 'COP')
ON CONFLICT (target_year, target_month, category) WHERE target_month IS NOT NULL DO NOTHING;

SELECT 'dias agregados: ' || count(*) FROM seed_days;
SELECT 'cierres agregados: ' || count(*) FROM seed_sales;

COMMIT;
