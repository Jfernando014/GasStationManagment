# SCRUM-83 — HU-37: precios de la competencia

Primera entrega backend del equipo WhiteCode. Módulo `competition`, independiente de HU-20/HU-21.

## Alcance e integración

- Alta, consulta y actualización identificativa de competidores; activación/desactivación sin borrado físico.
- Captura manual e histórica de precios; consulta paginada del histórico y último sondeo por estación/combustible.
- Comparación actual o a una fecha de corte con el promedio y mínimo de competidores actualmente activos.
- Simulación transitoria de precios y margen bruto estimado con costo de adquisición explícito opcional.
- Todas las operaciones HTTP requieren JWT y autoridad literal `ADMIN`.
- Sin frontend, ventas de canastilla, HU-20/HU-21, datos de demostración ni persistencia de escenarios.

Se reutilizan `inventory.FuelType`, `FuelPriceQuery` y `FuelPriceInfo`. Los únicos combustibles admitidos son
`MOTOR`, `DIESEL`, `EXTRA` y `MAX_PRO` (no `MAXPRO`). El módulo no importa entidades, servicios ni repositorios
internos de inventory; Spring inyecta la implementación existente de `FuelPriceQuery`.
No se cambian las APIs de inventario, metas, promedios, escenarios ni ventas.

## V18 y persistencia

Única migración: `backend/src/main/resources/db/migration/V18__create_competitor_price_surveys.sql`.

| Tabla | Columnas |
| --- | --- |
| `competitor` | `id BIGINT GENERATED ALWAYS AS IDENTITY`, `name VARCHAR(150)`, `brand VARCHAR(100)`, `location VARCHAR(255)`, `active BOOLEAN DEFAULT TRUE` |
| `competitor_price_observation` | `id BIGINT GENERATED ALWAYS AS IDENTITY`, `competitor_id BIGINT`, `fuel_type VARCHAR(20)`, `price_per_gallon NUMERIC(12,2)`, `observed_at TIMESTAMPTZ`, `created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP` |

Todos los campos son NOT NULL. Textos identificativos no vacíos. FK sin cascada de eliminación: impide eliminar
una estación con observaciones. Precios positivos y finitos; CHECK de combustibles y fechas finitas con
`observed_at <= created_at`. Índices:

- `(competitor_id, fuel_type, observed_at DESC, id DESC)`: selección del último sondeo e historia de una estación.
- `(observed_at DESC, id DESC)`: recorrido paginado del histórico general por fecha.

No hay INSERT de datos de producción, cambios a V1–V17, V19 ni duplicación de `fuel_price`.
Los sondeos son append-only en la API: no hay PUT/PATCH/DELETE de observaciones. `createdAt` lo genera PostgreSQL;
`observedAt` lo proporciona obligatoriamente el cliente. Registrar hoy una observación de una fecha anterior
no altera la fecha real del sondeo. Se admiten varios sondeos del mismo combustible/estación/instante;
el mayor ID resuelve empates de forma determinista.

Desactivar excluye una estación de últimas observaciones por defecto y de comparaciones, pero mantiene su historial.
Nuevas observaciones en estaciones inactivas producen 409. Se permite reactivarlas. Los cambios de estado y
el registro de observaciones usan bloqueo de la fila del competidor para serializar esas operaciones concurrentes.

## API REST

Base: `/api/v1/competition`. Encabezados: `Authorization: Bearer <JWT>` y, para cuerpos JSON,
`Content-Type: application/json`. OpenAPI documenta el esquema HTTP Bearer y las operaciones.

| Método | Ruta relativa | Contrato |
| --- | --- | --- |
| POST | `/competitors` | `{name, brand, location}`. 201, competidor activo y Location. |
| GET | `/competitors` | Filtro opcional `active=true/false`; omitido incluye ambos estados. Lista ordenada por nombre/ID. |
| GET | `/competitors/{id}` | Competidor o 404. |
| PUT | `/competitors/{id}` | Reemplaza `{name, brand, location}`; conserva estado e históricos. |
| PATCH | `/competitors/{id}/status` | `{active: false}` o `{active: true}`. |
| POST | `/observations` | `{competitorId, fuelType, pricePerGallon, observedAt}`. 201 y Location. |
| GET | `/observations/{id}` | Observación histórica o 404, incluso de una estación inactiva. |
| GET | `/observations` | Filtros opcionales `competitorId`, `fuelType`, `from`, `to`, `page=0`, `size=100`. |
| GET | `/observations/latest` | Filtros opcionales `competitorId`, `fuelType`, `asOf`, `includeInactive=false`. |
| GET | `/comparison` | `fuelType` opcional (omitido: los cuatro combustibles); `asOf` opcional (ahora). |
| POST | `/simulation` | `{fuelType, proposedPrice, acquisitionCost?, asOf?}`. 200; no persiste cambios. |

PUT actualiza toda la información identificativa. PATCH es la operación de baja/alta lógica, por lo que no
se expone DELETE físico. Los IDs son enteros positivos. Un filtro con un competidor inexistente devuelve 404.
Una lista sin observaciones devuelve `[]` y una página vacía conserva `totalElements: 0`.

Fechas HTTP: ISO-8601 con zona, p. ej. `2020-08-01T10:30:00-05:00` o `2020-08-01T15:30:00Z`.
Se normalizan a UTC y a precisión de microsegundos de PostgreSQL. No se aceptan fechas futuras ni anteriores
al año 1. Para períodos, `from` y `to` son inclusivos y `from <= to`; si se omiten, se usa desde el año 1
hasta ahora. `page >= 0`; `size` entre 1 y 200. El orden del histórico es observación descendente y luego ID descendente.
En URLs, codificar el signo `+` de un offset; Angular HttpParams realiza la codificación.

Precios: números JSON positivos, como máximo diez dígitos enteros y dos decimales (máximo 9999999999.99).
Costo explícito: mismo rango, admite cero. No se redondean entradas con más de dos decimales.
Nombre, marca y ubicación son obligatorios; se eliminan espacios exteriores. No se infiere una marca ni ubicación
si el cliente las omite. Nombre no es clave natural: estaciones homónimas son posibles.

### Ejemplos

Alta (`POST /competitors`):

```json
{"name":"Estación vecina","brand":"Independiente","location":"Vía principal, km 5"}
```

Respuesta 201 (ID ilustrativo, no dato insertado por migración):

```json
{"id":1,"name":"Estación vecina","brand":"Independiente","location":"Vía principal, km 5","active":true}
```

Captura (`POST /observations`):

```json
{"competitorId":1,"fuelType":"MOTOR","pricePerGallon":16000.50,"observedAt":"2020-08-01T10:30:00-05:00"}
```

Respuesta 201, con fecha de registro ilustrativa:

```json
{"id":10,"competitorId":1,"fuelType":"MOTOR","pricePerGallon":16000.50,"observedAt":"2020-08-01T15:30:00Z","createdAt":"2020-08-02T12:00:00Z"}
```

Histórico: `GET /observations?competitorId=1&fuelType=MOTOR&from=2020-08-01T00:00:00Z&to=2020-08-02T23:59:59Z&page=0&size=50`.

```json
{"content":[{"id":10,"competitorId":1,"fuelType":"MOTOR","pricePerGallon":16000.50,"observedAt":"2020-08-01T15:30:00Z","createdAt":"2020-08-02T12:00:00Z"}],"page":0,"size":50,"totalElements":1}
```

Comparación: `GET /comparison?fuelType=MOTOR&asOf=2020-08-02T20:00:00Z`.
Ejemplo con dos estaciones cuyos últimos precios son 100 y 120, y precio propio vigente de 121:

```json
{
  "asOf":"2020-08-02T20:00:00Z",
  "bellavistaDate":"2020-08-02",
  "mode":"AS_OF",
  "unit":"COP_PER_GALLON",
  "activeCompetitorsOnly":true,
  "fuels":[{
    "fuelType":"MOTOR",
    "bellavistaPrice":121.00,
    "bellavistaValidFrom":"2020-08-01",
    "competition":{"competitorCount":2,"average":110.000000,"minimum":100.00},
    "versusAverage":{"absoluteCopPerGallon":11.000000,"percentage":10.000000},
    "versusMinimum":{"absoluteCopPerGallon":21.000000,"percentage":21.000000},
    "observations":[
      {"id":1,"competitorId":1,"fuelType":"MOTOR","pricePerGallon":100.00,"observedAt":"2020-08-01T15:00:00Z","createdAt":"2020-08-01T16:00:00Z"},
      {"id":2,"competitorId":2,"fuelType":"MOTOR","pricePerGallon":120.00,"observedAt":"2020-08-02T15:00:00Z","createdAt":"2020-08-02T16:00:00Z"}
    ]
  }]
}
```

Sin `asOf`, `mode` es `CURRENT_LATEST`. Se incluyen los IDs, precios y fechas de los sondeos usados;
Angular puede relacionar `competitorId` con el catálogo de estaciones y mostrar la antigüedad.

Simulación (`POST /simulation`):

```json
{"fuelType":"MOTOR","proposedPrice":125.00,"acquisitionCost":100.00,"asOf":"2020-08-02T20:00:00Z"}
```

Respuesta con la misma competencia del ejemplo anterior:

```json
{
  "asOf":"2020-08-02T20:00:00Z",
  "fuelType":"MOTOR",
  "unit":"COP_PER_GALLON",
  "proposedPrice":125.00,
  "acquisitionCost":100.00,
  "competition":{"competitorCount":2,"average":110.000000,"minimum":100.00},
  "versusAverage":{"absoluteCopPerGallon":15.000000,"percentage":13.636364},
  "versusMinimum":{"absoluteCopPerGallon":25.000000,"percentage":25.000000},
  "estimatedMargin":{"grossCopPerGallon":25.00,"grossMarginPercentage":20.000000},
  "observations":[
    {"id":1,"competitorId":1,"fuelType":"MOTOR","pricePerGallon":100.00,"observedAt":"2020-08-01T15:00:00Z","createdAt":"2020-08-01T16:00:00Z"},
    {"id":2,"competitorId":2,"fuelType":"MOTOR","pricePerGallon":120.00,"observedAt":"2020-08-02T15:00:00Z","createdAt":"2020-08-02T16:00:00Z"}
  ]
}
```

Sin costo: `acquisitionCost: null`, `estimatedMargin: {grossCopPerGallon: null, grossMarginPercentage: null}`.
La diferencia competitiva puede seguir disponible aunque falte costo. Un mercado vacío produce conteo cero,
promedio/mínimo null y diferencias null, conservando el precio hipotético y el margen si se proporcionó costo.

## Reglas y fórmulas

1. Seleccionar sondeos con `observed_at <= asOf`, de competidores actualmente activos.
2. Por cada estación/combustible, elegir mayor `observed_at`; en empate, mayor ID. Nunca usar `created_at` para elegir el último sondeo.
3. Cada estación aporta un solo valor al promedio de ese combustible. No mezclar combustibles ni ponderar por número de registros.
4. Promedio = suma / cantidad de estaciones con precio; mínimo = menor precio seleccionado. Una estación sin ese combustible no entra al denominador.
5. Bellavista = `FuelPriceQuery.findEffectivePrice(fuelType, fechaBogota(asOf))`. No se usa el precio de hoy para una comparación a una fecha anterior.
6. Diferencia absoluta competitiva = precio Bellavista o hipotético − referencia de competencia.
7. Diferencia porcentual = diferencia / referencia de competencia × 100. Un valor positivo indica precio propio mayor.

Se usa BigDecimal en todos los importes. Promedio y diferencias derivadas usan seis decimales y HALF_EVEN;
los porcentajes son puntos porcentuales (10 representa 10 %, no 0.10). La diferencia se calcula frente al
promedio publicado a seis decimales. `absoluteCopPerGallon` es una diferencia monetaria **con signo**, no valor absoluto matemático.
Sin precio propio o referencia, ambas diferencias son null. Con referencia cero, el porcentaje es null;
las observaciones válidas de competencia no pueden tener precio cero.

### Margen financiero estimado

- Utilidad bruta estimada por galón = precio hipotético − costo de adquisición explícito.
- Margen bruto porcentual sobre venta = utilidad bruta / precio hipotético × 100.
- El precio hipotético es positivo, por lo que el denominador del margen no puede ser cero.
- Se aceptan pérdidas (costo mayor que precio) y costo cero explícito.
- No es markup sobre costo, ganancia neta, margen de metas ni diferencia frente a competencia.
- No incluye impuestos, transporte, comisiones, inventario contable ni gastos operativos.
- El costo es una entrada del escenario para ese combustible; no se consulta ni supone un costo histórico.

## Errores y seguridad

Errores del controlador en formato Problem Details: `status`, `title`, `detail`, `instance`; errores de dominio
incluyen `code` y validaciones de cuerpo incluyen `errors: [{field, message}]`. Tipos/fechas/JSON inválidos
producen 400 mediante ResponseEntityExceptionHandler. No se filtran mensajes SQL internos.

| Estado | Caso |
| --- | --- |
| 400 | Campos obligatorios, importes, enum, fecha, ID o paginación inválidos. |
| 403 | JWT autenticado sin autoridad literal ADMIN. El filtro global existente también devuelve 403 cuando falta JWT. |
| 404 | Competidor u observación inexistentes. |
| 409 | Nueva observación de estación inactiva o conflicto de integridad. |

La cadena JWT existente se conserva. El controlador aplica un guard ADMIN explícito en cada operación,
siguiendo la práctica de sales: no depende de anotaciones de seguridad de métodos deshabilitadas.
No se modifica el tratamiento global de tokens inválidos/expirados.

## Limitaciones y supuestos

- La zona es el conjunto de competidores activos registrados. `location` es referencia textual; no hay radio geográfico ni filtro por coordenadas.
- No hay antigüedad máxima aprobada. `CURRENT_LATEST` significa último precio observado, no garantiza que siga vigente hoy.
- Bellavista tiene vigencia diaria, no intradiaria. Se usa America/Bogota para elegir la fecha.
- `AS_OF` filtra por fecha real de observación; un sondeo ingresado después puede modificar retrospectivamente la comparación.
- El estado activo y la identificación del competidor son actuales; no se historizan. Una comparación pasada no reconstruye el padrón activo de ese momento.
- Los precios propios pueden corregirse con la API existente de inventory. No se promete reproducibilidad inmutable de una comparación histórica.
- No hay edición/borrado de sondeos ni flujo de anulación; registrar otra observación no elimina la anterior.
- No hay deduplicación por nombre ni idempotencia de importación. La captura es manual y admite múltiples observaciones.
- Los escenarios no persisten ni modifican precios propios, costos, metas o sondeos.

## Pruebas

Archivos nuevos bajo `backend/src/test/java/com/edu/unicauca/gasstation/backend/competition/`:

- `domain/CompetitionCalculationsTest`: BigDecimal, promedio, mínimo, signo, porcentajes, ausencia de datos y costo, cero, pérdidas y precisión.
- `domain/CompetitionServiceTest`: identidad, actualización, estados, fecha informada, identificadores, rango y paginación.
- `domain/CompetitionMarketServiceTest`: contrato público inventory, fecha Bogotá, ausencia de datos, simulación y costo explícito.
- `CompetitionModuleTest`: verificación real de Spring Modulith, límites y ausencia de ciclos.
- `api/CompetitionApiIntegrationTest`: HTTP, JWT real, persistencia JPA, filtros, empates, histórico, simulación sin escrituras, restricciones SQL y migración.

El test de integración usa exclusivamente PostgreSQL 16 `postgres:16-alpine` de Testcontainers con
`@ServiceConnection`, credenciales generadas y `ddl-auto=validate`. Aplica V1–V18 desde cero y prueba también
V17→V18 en otro esquema del mismo contenedor desechable, preservando productos/precios y comprobando repetición sin migraciones pendientes.
No usa gasstation_db ni gasstation_db_v2. No cambia Docker Compose ni variables de entorno.

Resultados de ejecución (22/10/2026):

| Verificación | Resultado |
| --- | --- |
| `mvn.cmd -o "-DskipTests" compile` | BUILD SUCCESS, Java 21. |
| `mvn.cmd -o "-Dtest=CompetitionCalculationsTest,CompetitionServiceTest,CompetitionMarketServiceTest,CompetitionModuleTest" test` | 25 pruebas aprobadas; cero fallos, errores u omisiones. |
| `mvn.cmd -o "-Dtest=CompetitionApiIntegrationTest" test` | 15 pruebas aprobadas; cero fallos, errores u omisiones. PostgreSQL 16.13 real, V1–V18 y V17→V18. |
| `mvn.cmd -o "-Dtest=FuelPriceServiceTest,FuelPriceControllerTest,FuelPriceRepositoryTest,SalesTargetControllerTest,SalesTargetStatisticsControllerTest,SalesTargetApiIntegrationTest" test` | 80 regresiones aprobadas; cero fallos, errores u omisiones. BUILD SUCCESS. |

El primer intento de integración no pudo comenzar porque Docker Desktop estaba detenido. Después de que el
usuario lo iniciara, la ejecución terminó correctamente. Maven se ejecutó fuera del sandbox para acceder a su
caché local; no se descargaron dependencias. Persisten advertencias del proyecto sobre Flyway 9.22.3/PostgreSQL 16,
agente Mockito y cierre de `eventPublicationRegistry` en el contexto JPA reducido; no produjeron fallos de pruebas.

Total: **120 pruebas aprobadas**, 40 nuevas y 80 de regresión. En el cierre de la regresión existente,
Hikari intentó cerrar el contexto JPA después de detenerse su contenedor y Surefire terminó el proceso hijo
tras 30 segundos de espera de salida. Maven devolvió código 0 y BUILD SUCCESS; los 80 casos terminaron
sin fallos. No se modificaron esos fixtures de otros módulos ni configuración para ocultar las advertencias.

## Consumo posterior desde SCRUM-84 / Angular

1. Usar el interceptor JWT existente y una pantalla autorizada para ADMIN.
2. Cargar `/competitors` para resolver nombres por ID; administrar identidad y estado mediante las rutas anteriores.
3. Enviar `observedAt` con offset/UTC obtenido de la fecha real informada; no sustituirlo por la fecha del envío.
4. Usar `/observations` para tablas paginadas y `/observations/latest` para sondeos recientes.
5. Consumir `/comparison`; dibujar series a partir de `fuels`, mostrar conteo y fechas y representar null como «Sin datos».
6. Enviar precios hipotéticos a `/simulation`; presentar por separado diferencias competitivas y margen bruto estimado.
7. Usar `HttpParams` para fechas con offset y filtros. Diferenciar errores HTTP de resultados vacíos; un error no es cero.

Ejemplo mínimo de llamada desde un servicio Angular (sin cambios de frontend en esta entrega):

```typescript
this.http.get<ComparisonResponse>(`${apiUrl}/competition/comparison`, {
  params: { fuelType: 'MOTOR' }
});
this.http.post<SimulationResponse>(`${apiUrl}/competition/simulation`, {
  fuelType: 'MOTOR', proposedPrice: 125.00, acquisitionCost: 100.00
});
```

Los DTOs de Angular deben reflejar los ejemplos y campos anulables. Evitar aritmética financiera con Number;
usar una librería decimal o dejar los cálculos al backend. Los IDs BIGINT siguen la convención numérica actual;
si se superase Number.MAX_SAFE_INTEGER, el cliente necesitaría lectura JSON sin pérdida de precisión.

## Archivos de esta entrega

Solo se añaden archivos nuevos: la migración V18, este documento, 22 archivos de producción dentro de
`competition/` (API/DTOs, modelos, puertos, servicios, excepciones, JPA/adaptadores y declaración de módulo)
y cinco archivos de pruebas. No se modifican archivos existentes de otros módulos, frontend, configuración,
Docker ni migraciones anteriores. `docs/references/` permanece local y no se incluye en ninguna operación Git.

Archivos de producción, relativos a `backend/src/main/java/com/edu/unicauca/gasstation/backend/competition/`:

| Directorio | Archivos nuevos |
| --- | --- |
| Raíz | `package-info.java` |
| `api/` | `CompetitionController.java`, `CompetitionAdminAuthorization.java`, `CompetitionExceptionHandler.java`, `CompetitionApiDocumentation.java` |
| `api/dto/` | `CompetitionRequests.java` |
| `domain/models/` | `Competitor.java`, `PriceObservation.java`, `PriceCalculations.java` |
| `domain/repositories/` | `CompetitorRepository.java`, `ObservationRepository.java` |
| `domain/services/` | `CompetitionService.java`, `MarketPriceService.java` |
| `exception/` | `CompetitorNotFoundException.java`, `ObservationNotFoundException.java`, `InactiveCompetitorException.java` |
| `infrastructure/persistence/entities/` | `CompetitorEntity.java`, `ObservationEntity.java` |
| `infrastructure/persistence/repositories/` | `JpaCompetitorRepository.java`, `JpaObservationRepository.java` |
| `infrastructure/persistence/adapters/` | `CompetitionRepositoryAdapter.java`, `ObservationRepositoryAdapter.java` |
