# SCRUM-83 — HU-20 y HU-21 (segunda entrega backend)

## Estado y evidencia inicial

Rama: `feature/back/sales-statistics-scrum83`. HEAD inicial:
`8f88b3800bfeed649e78b1ec36505d4a04cd4046`, igual a la referencia local
`origin/main`. No se hizo fetch ni se presume un estado remoto posterior.
El único contenido sin seguimiento al iniciar fue `docs/references/`, conservado
sin modificar ni añadir al índice.

Se consultó el archivo local `docs/references/Historias Usuario Estacion.xlsx`:

- HU-20, hoja 5, fila 24: detalle y total de venta de canastas por vendedor al
  seleccionar un período.
- HU-21, hoja 5, fila 26: gráficas de ventas, cumplimiento y comparaciones,
  mediante barras o líneas según el indicador.

Ese archivo es material de consulta local; no forma parte de esta entrega.
También se revisó `docs/SCRUM-40-implementation.md` y el código integrado.
**Las historias no están completas como funcionalidad de ventas históricas:**
todavía falta conectar la captura real del cierre y la asistencia verificable.

| Componente actual | Evidencia y uso en esta entrega |
| --- | --- |
| `SalesTargetService` | Metas configuradas, nunca ventas realizadas. Se reutiliza para leer la meta de una gráfica. |
| `SalesDistributionService` | Reparto de metas con perfil almacenado y asignaciones explícitas; se reutiliza sin recalcular ni cambiar su algoritmo. |
| `SalesJourneyStatistics` | Promedios y escenarios sobre observaciones explícitas. Se reutiliza para validar unicidad fecha/jornada y conservar sus unidades y semántica de observaciones. |
| `HistoricalDayAverages` | Motor existente sobre `SalesHistoryReadPort`; excluye totales diarios cero de su denominador. Se conserva y prueba; no se conecta a datos ficticios ni se confunde con el promedio por jornada. |
| `SalesStatisticsController` | Se conservan sus dos endpoints y sus DTOs. |
| `SalesHistoryReadPort` | Sin implementación integrada. Solo contiene total por fecha/categoría agregada; no permite atribuir ventas a vendedores, productos o turnos. |
| `SalesAssignmentReadPort` | Sin implementación integrada. Su contrato de asignaciones programadas no acredita asistencia. |
| Workers | `ScheduleWorkerDirectory`, declarado públicamente por shifts e implementado por workers, permite resolver UUID y nombre en lote. Conserva los inactivos. No se importan repositorios internos de workers. |
| Shifts | Catálogo público `ShiftExternalService` y programación. `ShiftAssignment` representa programación, incluido descanso; no existe un contrato de asistencia efectiva. |
| Inventory | Enum público `ProductCategory`: `LUBRICANTE`, `ADITIVO`, `REFRIGERANTE`, `OTRO`. El producto y su precio actual no prueban una venta ni su precio histórico. |
| Journals | `Journal`, `JournalStep`, `JournalSnapshot` y `JournalReadPort` contienen fecha/estado/pasos; no exponen líneas de venta por vendedor ni asistencia. Un paso completado o un archivo adjunto no se interpreta como ventas estructuradas. |
| Statistics | El módulo contiene ejemplos; esta entrega extiende la funcionalidad existente en sales, sin crear otra fuente de ventas. |

La ausencia se comprobó en entidades, servicios y contratos del checkout. No se
consultaron las bases locales para afirmar qué filas contienen. No se usan los
datos del prototipo como ventas reales.

## Alcance y disponibilidad

| Funcionalidad | Implementación | Disponibilidad con este checkout |
| --- | --- | --- |
| HU-20: rango, vendedor UUID, categoría | Endpoint, validación, filtros y agrupación vendedor/categoría | Contrato disponible; datos de ventas `UNAVAILABLE` |
| Importes COP, unidades de producto, participación | Cálculo exacto de totales y porcentaje sobre el total filtrado | Requiere `BasketSalesReadPort` conectado y completo |
| Promedio por turno trabajado | Denominador independiente de asistencia verificada, incluidos turnos sin ventas | Requiere además `WorkedShiftsReadPort` |
| Barras por vendedor/categoría | Importes, cantidades, participación y promedio por turno | Requiere las fuentes indicadas arriba |
| Series reales por vendedor/categoría | Totales por día, semana o mes y cambios entre períodos observados | Requiere ventas reales |
| HU-21: tendencias observadas | Líneas de totales y promedios por jornada observada | Funciona hoy con observaciones proporcionadas por el cliente |
| Promedios diurna/nocturna y comparación de escenarios | Endpoints existentes de SCRUM-40 | Funcionan hoy con observaciones proporcionadas por el cliente |
| Barras de meta y venta máxima configuradas | Lectura mediante `SalesTargetService` | Funciona cuando existe la meta solicitada |
| Líneas por fecha y barras por vendedor de metas oficial/referencia | Proyección del resultado de `SalesDistributionService` | Funciona con meta mensual, perfil existente y asignaciones explícitas válidas |
| Cumplimiento real, comparación real/meta, histórico automático | No se calcula ni se presenta un porcentaje supuesto | Pendiente de captura real y reglas de alineación de períodos |
| Dibujar gráficas, exportar reportes | Corresponde al cliente u otras historias | No se modifica Angular ni se implementan exportaciones |

No se crea persistencia de ventas, adaptador de ejemplo ni migración. Tampoco se
agrega un endpoint para introducir ventas u observaciones de vendedor y hacerlas
pasar por histórico del servidor.

## Convenciones REST

Todos los endpoints nuevos requieren `Authorization: Bearer <JWT>` y la autoridad
literal `ADMIN`, como los cálculos de SCRUM-40. `ROLE_ADMIN` y `READ_ONLY` no bastan.
Se usa el filtro JWT existente y `SalesAdminAuthorization`; no se depende de
anotaciones de seguridad de métodos que no estén habilitadas.

| Método y ruta | Contrato |
| --- | --- |
| `GET /api/v1/sales/statistics/basket` | `BasketStatisticsResponse` |
| `POST /api/v1/sales/statistics/trends/explicit` | `ExplicitTrendRequest` → `StatisticsChartResponse` |
| `GET /api/v1/sales/statistics/targets/{id}` | `StatisticsChartResponse` con meta almacenada |
| `POST /api/v1/sales/statistics/targets/{id}/distribution` | DTO existente `CalculateDistributionRequest` → `StatisticsChartResponse` |

Procedencias (`source`):

- `CLIENT_SUPPLIED_OBSERVATIONS`: datos enviados en esa solicitud; no se guardan,
  no se certifican y no se convierten en histórico real.
- `TARGET_PROJECTION`: configuración o reparto de metas; no mide cumplimiento.
- `REAL_HISTORICAL_SALES`: exclusivamente el puerto del cierre responsable. Si
  falta el puerto, el campo describe la fuente requerida, con estado indisponible.

Estados (`availability`):

- `AVAILABLE`: existen valores de la fuente indicada. Un cero registrado sigue
  siendo un dato y puede aparecer en la gráfica.
- `NO_DATA`: la fuente está conectada y declara completo el período, pero no hay
  registros para los filtros; o la solicitud explícita no contiene observaciones.
  No certifica una venta igual a cero.
- `UNAVAILABLE`: no hay fuente conectada o la fuente declara que no dispone del
  período completo. No contiene ceros sustitutos.

En canastilla la procedencia y disponibilidad están en `charts`; el estado de
asistencia es independiente (`workedShiftsAvailability`, `workedShiftsReason`).
`source`, `availability`, `chartType`, `metric`, `unit` y la categoría de cada
serie son enums en los DTOs Java y en el esquema OpenAPI.
Sin ventas, `summary=null`, `groups=[]`, `charts.series=[]`; no se consulta
asistencia ni se adivinan vendedores a partir de programación.

Errores: parámetros, rangos y JSON inválidos producen 400; meta/perfil inexistente
produce 404; falta de autoridad produce 403. El filtro actual también devuelve
403 para solicitudes anónimas. El guard local puede producir 401 si se invoca sin
autenticación fuera de ese filtro. Un snapshot nulo, IDs de líneas/turnos duplicados
o fechas fuera del rango en una fuente conectada producen 502 con código
`INVALID_STATISTICS_SOURCE`. Se conserva el código heredado `INVALID_SALES_TARGET`
del handler compartido para errores `IllegalArgumentException`, también en reportes.
Los errores de validación al construir registros dentro de un adaptador también
se clasifican como fallo de la fuente (502), sin atribuirlos a la solicitud válida.

### Consulta de canastilla

```http
GET /api/v1/sales/statistics/basket?from=2026-08-01&to=2026-08-31&period=MONTH&outputScale=2
Authorization: Bearer <JWT_ADMIN>
```

Filtros opcionales: `workerId=<UUID no nulo>`,
`category=LUBRICANTE|ADITIVO|REFRIGERANTE|OTRO`. `period=DAY|WEEK|MONTH`, por
defecto `DAY`; `outputScale` por defecto 2. `from` y `to` son obligatorios e
inclusivos. El UUID de todos ceros no es válido; un UUID sin ventas produce
`NO_DATA` cuando la fuente existe, no se infiere una identidad inexistente.

Respuesta actual sin adaptadores conectados:

```json
{
  "from": "2026-08-01", "to": "2026-08-31",
  "workerId": null, "category": null, "period": "MONTH",
  "moneyUnit": "COP", "quantityUnit": "PRODUCT_UNITS",
  "workedShiftsAvailability": "UNAVAILABLE",
  "workedShiftsReason": "NOT_EVALUATED_WITHOUT_SALES",
  "summary": null, "groups": [],
  "charts": {
    "source": "REAL_HISTORICAL_SALES", "availability": "UNAVAILABLE",
    "reason": "SALES_SOURCE_NOT_CONNECTED",
    "from": "2026-08-01", "to": "2026-08-31",
    "outputScale": 2, "roundingMode": "HALF_EVEN",
    "decimalPolicy": "EXACT_TOTALS_HALF_EVEN_DERIVED_VALUES",
    "projection": null, "series": []
  }
}
```

Con una fuente real futura, `summary` contiene `amount`, `quantity`, `saleLines`,
`workedShifts`, `averageAmountPerWorkedShift`. Cada grupo contiene `workerId`,
`workerName` (nullable), `category`, los totales, cantidad de líneas,
`participationPercent` y los campos de asistencia/promedio.

Se emiten líneas `RECORDED_AMOUNT`/`COP` y `RECORDED_QUANTITY`/`PRODUCT_UNITS`
por vendedor/categoría y barras agregadas de esas métricas. Se añaden barras
`PARTICIPATION`/`PERCENT` si el total es positivo y
`AVERAGE_PER_WORKED_SHIFT`/`COP_PER_WORKED_SHIFT` si la asistencia es verificable.
No se generan grupos de vendedores sin registros ni combinaciones vacías.
Las barras usan claves `workerId/category`, resolubles con `groups`; las líneas
incluyen `workerId` y `category` dentro de su serie.

### Tendencias sobre observaciones explícitas

Ejemplo numérico ilustrativo, enviado por el cliente, sin persistencia:

```http
POST /api/v1/sales/statistics/trends/explicit
Authorization: Bearer <JWT_ADMIN>
Content-Type: application/json

{
  "period": "DAY",
  "sales": {
    "from": "2026-08-01", "to": "2026-08-31",
    "category": "BASKET", "outputScale": 2,
    "observations": [
      {"date": "2026-08-01", "journey": "DAY", "value": 10},
      {"date": "2026-08-03", "journey": "DAY", "value": 20}
    ]
  }
}
```

```json
{
  "source": "CLIENT_SUPPLIED_OBSERVATIONS", "availability": "AVAILABLE", "reason": null,
  "from": "2026-08-01", "to": "2026-08-31", "outputScale": 2,
  "roundingMode": "HALF_EVEN", "decimalPolicy": "EXACT_TOTALS_HALF_EVEN_DERIVED_VALUES",
  "projection": null,
  "series": [
    {
      "id": "DAY/total", "label": "Diurna", "chartType": "LINE",
      "metric": "OBSERVED_TOTAL", "unit": "COP", "workerId": null, "category": null,
      "points": [
        {"key": "2026-08-01", "periodStart": "2026-08-01", "value": 10, "observations": 1, "change": null, "changePercent": null},
        {"key": "2026-08-03", "periodStart": "2026-08-03", "value": 20, "observations": 1, "change": 10, "changePercent": 100.00}
      ]
    },
    {
      "id": "DAY/average", "label": "Diurna / Promedio por observación", "chartType": "LINE",
      "metric": "AVERAGE_PER_JOURNEY_OBSERVATION", "unit": "COP", "workerId": null, "category": null,
      "points": [
        {"key": "2026-08-01", "periodStart": "2026-08-01", "value": 10.00, "observations": 1, "change": null, "changePercent": null},
        {"key": "2026-08-03", "periodStart": "2026-08-03", "value": 20.00, "observations": 1, "change": null, "changePercent": null}
      ]
    }
  ]
}
```

No se introduce el día 2 con valor cero ni una serie nocturna ficticia. Con
`MONTH` esos dos puntos se consolidan en agosto: total 30, observaciones 2,
promedio por observación diurna 15. Una observación es un total ya consolidado
por fecha/jornada/categoría, no un turno de un vendedor. `FUEL_AGGREGATE` usa
`GALLONS`; `BASKET` usa `COP`, conforme al motor existente.

### Gráfica de meta configurada

El ID debe corresponder a una meta existente; los valores siguientes son solo
un ejemplo de contrato, no datos insertados por la entrega.

```http
GET /api/v1/sales/statistics/targets/42?outputScale=2
Authorization: Bearer <JWT_ADMIN>
```

Ejemplo para una meta mensual almacenada de 10.005 COP sin venta máxima:

```json
{
  "source": "TARGET_PROJECTION", "availability": "AVAILABLE", "reason": null,
  "from": null, "to": null, "outputScale": 2, "roundingMode": null,
  "decimalPolicy": "UNROUNDED_STORED_VALUES",
  "projection": {"targetId": 42, "profileId": null, "parameterSource": null, "assignmentSource": null, "uncoveredHourCount": null},
  "series": [{
    "id": "target", "label": "Meta configurada", "chartType": "BAR",
    "metric": "CONFIGURED_TARGET", "unit": "COP", "workerId": null, "category": null,
    "points": [{"key": "2026-08", "periodStart": null, "value": 10.005, "observations": 0, "change": null, "changePercent": null}]
  }]
}
```

El año solo identifica metas anuales; `año-mes` identifica las mensuales. No se
divide una meta anual. Si existe `maximumSale`, se incluye otra serie
`CONFIGURED_MAXIMUM_SALE`. `observations=0` indica que la configuración no aporta
observaciones de venta. `outputScale` no redondea los valores almacenados.

### Gráfica del reparto de metas

```http
POST /api/v1/sales/statistics/targets/42/distribution
Authorization: Bearer <JWT_ADMIN>
Content-Type: application/json

{
  "profileId": 7, "outputScale": 2,
  "assignments": [{
    "date": "2026-08-01", "endDate": "2026-08-31",
    "workerId": "00000000-0000-0000-0000-000000000001", "shiftCode": "DIA"
  }]
}
```

Los IDs de meta, perfil, trabajador y código deben existir y satisfacer las
reglas del reparto; el UUID del ejemplo no se crea. Se conserva el DTO de
SCRUM-40: `endDate` es opcional, repite el mismo código y debe permanecer dentro
del mes; no infiere rotaciones. El mes de la meta debe coincidir.

Fragmento de la respuesta, con valores ilustrativos:

```json
{
  "source": "TARGET_PROJECTION", "availability": "AVAILABLE",
  "reason": "PARTIAL_SHIFT_COVERAGE", "outputScale": 2, "roundingMode": null,
  "decimalPolicy": "EXACT_RATIONAL_LARGEST_REMAINDER_DATE_HOUR_UUID",
  "projection": {
    "targetId": 42, "profileId": 7, "parameterSource": "Descripción del perfil almacenado",
    "assignmentSource": "EXPLICIT_CALLER_SUPPLIED_WORKSHEET_DATE_ASSIGNMENTS",
    "uncoveredHourCount": 372
  },
  "series": [{
    "id": "OFFICIAL_TARGET/date", "label": "Meta oficial", "chartType": "LINE",
    "metric": "OFFICIAL_TARGET", "unit": "COP", "workerId": null, "category": null,
    "points": [{"key": "2026-08-01", "periodStart": "2026-08-01", "value": 100.00, "observations": 0, "change": null, "changePercent": null}]
  }]
}
```

La respuesta completa incluye cuatro series: oficial/fecha, oficial/vendedor,
referencia/fecha y referencia/vendedor. Las barras usan UUID como clave. Las
fechas conservan la atribución de hoja de cálculo del motor, sin atribuirles una
fecha de asistencia nocturna. `reason=null` cuando no hay horas sin cobertura.
La política de reparto y sus importes se copian del resultado del servicio, no
se vuelven a redondear. Para allocations, totales mensuales y el detalle de horas
sin cobertura se conserva `POST /api/v1/sales/targets/{id}/distribution`.

### Endpoints existentes de promedios y escenarios

Permanecen sin cambios:

```http
POST /api/v1/sales/averages/explicit
Authorization: Bearer <JWT_ADMIN>
Content-Type: application/json

{"from":"2026-08-01","to":"2026-08-31","category":"BASKET","outputScale":2,
 "observations":[{"date":"2026-08-03","journey":"DAY","value":10}]}
```

Fragmento de respuesta:

```json
{"source":"CLIENT_SUPPLIED_OBSERVATIONS","unit":"COP","outputScale":2,"roundingMode":"HALF_EVEN",
 "series":[{"label":"Diurna","dayOfWeek":null,"journey":"DAY","total":10,"observations":1,"available":true,
 "average":10.00,"exactAverage":{"numerator":"10","denominator":"1"}}]}
```

La respuesta completa también conserva la entrada nocturna con
`available=false`, `average=null` y `exactAverage=null` si no se aportó dato.
El `total=0` heredado de una entrada indisponible no acredita una venta cero;
consumir siempre su indicador `available`. No se cambia ese contrato existente.

```http
POST /api/v1/sales/scenarios/compare/explicit
Authorization: Bearer <JWT_ADMIN>
Content-Type: application/json

{"sales":{"from":"2026-08-01","to":"2026-08-31","category":"BASKET","outputScale":2,
 "observations":[{"date":"2026-08-03","journey":"DAY","value":10}]},
 "first":{"dayOfWeek":"MONDAY","journey":"DAY"},
 "second":{"dayOfWeek":"MONDAY","journey":"NIGHT"}}
```

El mismo `SalesStatisticsResponse` devuelve dos entradas ordenadas:
`lunes / Diurna` con promedio 10 y `lunes / Nocturna` indisponible. Angular puede
representar barras de `average` o `total` respetando `available` y `source`.
No se duplica el servicio de comparación.

## Reglas de cálculo y validación

1. Rangos nuevos inclusivos en años 1..9999, máximo técnico de 3660 días.
   `outputScale` entero entre 0 y 12. IDs de meta/perfil positivos; vendedor
   UUID válido y distinto del UUID cero. Categorías y períodos son enums.
   Los endpoints previos conservan sus validaciones originales.
2. Las solicitudes explícitas reutilizan `ExplicitSalesStatisticsRequest`,
   `Journey` y la categoría agregada existentes: fechas fuera del rango,
   observaciones nulas, importes negativos y duplicados fecha/jornada se rechazan.
3. Importes y cantidades se suman con `BigDecimal`, sin redondear los totales.
   COP es moneda; `PRODUCT_UNITS` cuenta unidades de los productos registrados,
   no número de canastas, litros ni galones. No se estima el importe mediante
   precio de catálogo ni se mezclan unidades físicas.
4. Participación de cada grupo = importe del grupo / importe total de la
   selección × 100. El denominador respeta filtros de vendedor/categoría.
   Si el total es cero, porcentaje `null`; no se emite barra de participación.
   Por redondeo independiente, porcentajes pueden no sumar exactamente 100.
5. Promedio de grupo = importe de vendedor/categoría / todos los turnos
   efectivamente trabajados por ese vendedor en el rango, aun sin ventas.
   Promedio total = importe seleccionado / todos los turnos de vendedores
   incluidos por el filtro de UUID; un filtro de categoría no elimina turnos
   sin ventas de esa categoría. Si no se filtra UUID, el total incluye también
   turnos verificados de vendedores sin líneas de venta. No se suma otra vez
   el denominador por cada categoría.
6. Si falta asistencia, su snapshot es indisponible o una fecha/vendedor con
   venta carece de turno verificado, todos los promedios por turno de la selección
   quedan `null`. Se conservan totales/series de ventas. El motivo distingue
   `WORKED_SHIFTS_SOURCE_NOT_CONNECTED`, `WORKED_SHIFTS_SOURCE_UNAVAILABLE` y
   `SALES_WITHOUT_VERIFIED_WORKED_SHIFT`. Nunca se usa la programación como prueba.
7. Cocientes, promedios y porcentajes nuevos usan `outputScale` y `HALF_EVEN`.
   Sumas y diferencias permanecen exactas. Los promedios de jornada existentes
   conservan además la fracción exacta. La configuración se devuelve intacta
   con `UNROUNDED_STORED_VALUES`; el reparto conserva su política exacta de
   mayor resto y su desempate original. `roundingMode=null` en esas proyecciones.
8. Períodos ordenados por fecha: día natural, semana de lunes a domingo o mes
   natural. La clave semanal/mensual es el inicio del período y puede preceder
   `from` en una semana/mes parcial; solo se suman registros dentro del rango.
   No se rellenan huecos ni se extrapolan períodos incompletos.
9. En series temporales, cambio = total actual − total observado anterior;
   cambio porcentual = cambio / total anterior × 100. Se compara el anterior
   **observado**, incluso si hay huecos. El primer punto no tiene cambio; una
   base cero no tiene cambio porcentual. Un único período no demuestra tendencia.
   No es regresión estadística ni pronóstico. Las series de promedio y las
   proyecciones no calculan cambios.
10. En líneas observadas, `observations` cuenta totales fecha/jornada; el
    promedio de período divide por ese conteo, incluidos ceros explícitos.
    En canastilla cuenta líneas de venta, nunca turnos ni días. En proyecciones
    vale cero porque no existen observaciones de venta.
11. Identificación por UUID; nombres actuales se consultan en lote mediante el
    contrato público. Si falta un nombre se conserva `workerName=null` y se usa
    el UUID en la etiqueta. Un vendedor inactivo conserva sus estadísticas.
    No se afirma que el nombre actual sea una instantánea histórica.

## Conexión posterior de las fuentes

Se añaden contratos públicos en el paquete raíz de sales para respetar Modulith:

```java
BasketSalesReadPort.Snapshot read(LocalDate fromInclusive, LocalDate toInclusive);
// Snapshot(boolean available, List<Sale> sales)
// Sale(String id, LocalDate date, UUID workerId, long productId,
//      ProductCategory category, BigDecimal amount, BigDecimal quantity)

WorkedShiftsReadPort.Snapshot read(LocalDate fromInclusive, LocalDate toInclusive);
// Snapshot(boolean available, List<WorkedShift> shifts)
// WorkedShift(String id, UUID workerId, LocalDate date)
```

El módulo dueño del cierre puede registrar un bean de `BasketSalesReadPort`.
El dueño de la asistencia puede registrar uno de `WorkedShiftsReadPort`. Ninguno
es obligatorio para iniciar la aplicación; `ObjectProvider` distingue ausencia
de integración. No se registra un proveedor vacío como si fuera histórico.
Debe existir como máximo una implementación de cada puerto.

Obligaciones del adaptador:

- `available=true` acredita cobertura completa y datos finales de todo el rango,
  no una página parcial. Si faltan cierres o no puede acreditar cobertura,
  devolver `available=false` y lista vacía. Lista vacía con `available=true`
  significa que no hay líneas capturadas para ese rango completo.
- IDs estables y únicos de líneas y turnos, sin duplicados por joins o eventos.
  Cada turno realmente trabajado tiene un único ID. La asistencia contiene solo
  turnos de vendedores, no personal que no vende, e incluye turnos sin ventas.
- `date` es fecha operativa, con la misma atribución nocturna en ambos puertos;
  esta entrega no la deduce de instantes ni códigos programados.
- `amount` es el importe COP registrado en esa venta. `quantity` son unidades
  vendidas de producto; `category` es la categoría al vender. Ambos valores son
  no negativos; no se aplica automáticamente el precio/categoría actual.
- Vendedores y productos deben tener identidades verificadas por el módulo de
  captura; este lector no crea identidades ni utiliza nombres como llaves.
- Devoluciones, cancelaciones y ajustes negativos necesitan reglas aprobadas
  antes de ampliar el contrato; no se ocultan ni se convierten a valores absolutos.
- El adaptador pertenece al módulo responsable y debe usar sus propios recursos
  o contratos públicos. No debe importar internos de sales ni introducir ciclos.

`SalesHistoryReadPort` sigue reservado para totales históricos de estación;
no se reemplaza por este contrato más detallado. `SalesAssignmentReadPort` sigue
reservado para programación y no implementa `WorkedShiftsReadPort` por equivalencia.

## Integración Angular

- Las fechas son cadenas ISO sin hora. Evitar convertir `periodStart` a hora
  local mediante un `Date` UTC que desplace la fecha en Colombia.
- Mostrar estado/motivo antes de intentar dibujar. No convertir `null` a cero
  ni completar días ausentes. La nueva respuesta de canastilla usa `charts`;
  el nuevo endpoint de tendencias devuelve el objeto de gráfica directamente.
- Mantener distintas las gráficas COP, cantidades y porcentajes según `unit` y
  `metric`; títulos y leyendas deben reflejar `source`. No etiquetar una meta o
  una observación del cliente como venta histórica certificada.
- `chartType` es una sugerencia de presentación. Las barras por vendedor usan
  UUID o `UUID/categoría` como clave; resolver nombres sin perder su identidad.
- Los endpoints de SCRUM-40 mantienen su formato distinto con `series.average`
  y `available`; los nuevos tienen `series.points`. No sustituir un tipo por otro.
- Los números JSON salen de `BigDecimal`; JavaScript `number` puede perder
  precisión en importes grandes. Usar una estrategia decimal en Angular si se
  necesitan operaciones exactas; no recalcular totales monetarios con flotantes.
- Hay que proveer JWT con `ADMIN`; se conserva la configuración CORS existente.
  No se modificaron clientes, interceptores ni configuración del frontend.

## Archivos de la entrega

Prefijo Java: `backend/src/main/java/com/edu/unicauca/gasstation/backend/sales/`.

| Archivo creado | Responsabilidad |
| --- | --- |
| `BasketSalesReadPort.java` | Contrato público de ventas reales y snapshot de cobertura. |
| `WorkedShiftsReadPort.java` | Contrato público de turnos verificados. |
| `api/SalesReportingController.java` | Cuatro endpoints ADMIN. |
| `api/dto/BasketStatisticsResponse.java` | Grupos, totales y disponibilidad de asistencia. |
| `api/dto/ExplicitTrendRequest.java` | Reutiliza observaciones de SCRUM-40 y añade período. |
| `api/dto/StatisticsChartResponse.java` | Series, puntos, unidades, fuente, disponibilidad y contexto del reparto. |
| `application/BasketStatisticsService.java` | Orquestación de puertos, filtros, grupos y promedios. |
| `application/SalesChartsService.java` | Adaptación de motores existentes a series. |
| `domain/calculation/StatisticsPeriod.java` | Agrupación temporal y validación de rango/escala. |
| `domain/calculation/StatisticsSeries.java` | Buckets observados, totales, promedios y cambios. |
| `exception/StatisticsSourceException.java` | Inconsistencia del contrato de una fuente conectada. |

Único archivo existente modificado:
`api/SalesTargetExceptionHandler.java`, para incluir el nuevo controlador y el
error 502 de fuentes. No se cambia el comportamiento de sus errores anteriores.

Pruebas nuevas bajo el prefijo equivalente de `backend/src/test/java/`:
`sales/application/BasketStatisticsServiceTest.java`,
`sales/application/SalesChartsServiceTest.java`,
`sales/api/SalesReportingControllerTest.java`,
`sales/SalesStatisticsModularityTest.java`.
Documento nuevo: `docs/SCRUM-83-sales-statistics.md`.

## Validación ejecutada

Java 21.0.8, Spring Boot 3.2.4, Maven 3.9.11. Maven necesitó acceso fuera del
sandbox para leer su caché local. No se cambiaron dependencias ni configuración.

Primero se ejecutó una compilación limpia (`mvn.cmd -o clean` con la misma
selección de pruebas), que pasó 212 casos. Después de los ajustes finales de
enums y validación de fuentes, se ejecutó desde `backend`:

```powershell
mvn.cmd -o "-Dtest=SalesReportingControllerTest,SalesTarget*Test,!SalesTargetMigrationTest,SalesStatisticsModularityTest,BasketStatisticsServiceTest,SalesChartsServiceTest" test
```

**215 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS.**

- 21 casos HTTP nuevos: JWT y autoridad en las cuatro rutas, datos observados,
  indisponibilidad, clasificación, filtros, rangos, escala, IDs, 404/502,
  preservación de importes y diagnóstico de cobertura en proyecciones.
- 16 casos del servicio de canastilla: agrupaciones, filtros inclusivos,
  participación, ceros, ausencia de fuentes/datos, asistencia completa/inconsistente,
  turnos sin ventas, errores de validación del adaptador, precisión de porcentajes
  e identidades/nombres de vendedores.
- 8 casos de gráficas: orden temporal, semanas lunes/domingo, agregación mensual,
  cambio con base cero, huecos, precisión, compatibilidad con promedios y reparto.
- 1 verificación global con
  `ApplicationModules.of(GasStationApplication.class).verify()`: límites públicos
  y ausencia de ciclos comprobados sin excluir módulos.
- 169 pruebas existentes: metas, almacenamiento, validaciones, mapeadores,
  programación explícita, reparto, máximo, promedios y escenarios. Incluye 10
  casos de integración API y 5 de repositorio con PostgreSQL 16 desechable de
  Testcontainers, configurado mediante `@ServiceConnection`.

No se ejecutó toda la suite del repositorio. `SalesTargetMigrationTest` se excluyó
porque fija como último esquema V12 y no corresponde al alcance sin migraciones
de esta entrega; no se modificó ese test. Las regresiones de API/repositorio sí
ejecutaron los recursos integrados contra sus bases desechables.

Una ejecución previa encontró un recurso residual de otra rama en `target`; se
descartó esa evidencia y se repitió con `clean`. La ejecución final usa únicamente
los recursos de este checkout. No se copió código ni recursos del PR de HU-37.
Las advertencias existentes de instrumentación Java/Flyway no causaron fallos.

Se comprobó `git diff --check`. No se ejecutaron `git add`, commit, push, merge o
rebase; rama y HEAD permanecen iguales. No se crearon ni editaron migraciones,
credenciales, Docker, frontend o funcionalidad de otros integrantes. No se
accedió ni alteró `gasstation_db` o `gasstation_db_v2`; las únicas escrituras de
base de datos fueron las de pruebas en contenedores nuevos y desechables.
