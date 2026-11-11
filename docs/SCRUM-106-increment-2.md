# SCRUM-106 — Incremento 2: integraciones

## Estado de la rama y comparación con main

Trabajo realizado en `repo-scrum106`, rama `feature/back/scrum-106-performance`.
Se ejecutó `git fetch origin` el 2026-11-11 y se comparó:

- HEAD: `9568be05b6f226b9035f8654724389155e972168`.
- origin/main: `f84adc677314139b9930ef0860471ba227230379`.
- `git rev-list --left-right --count HEAD...origin/main`: `0 25`.
- main ya contiene SCRUM-107 / PR #76 (`2ce75bc`) y PR #77 / #78.

Los cambios locales del incremento anterior se conservaron. No se ejecutaron merge,
rebase, reset, cambio de rama, commit ni push. Solo el fetch actualizó referencias
remotas y sus metadatos Git. No se escribieron archivos de otros worktrees ni se
inició la aplicación contra la base de desarrollo.

No se encontraron archivos de producción con cambios de main que coincidan con
los archivos modificados o añadidos para SCRUM-106. Esto es una comparación de rutas
y contratos, no una validación de una integración de ramas. Puntos a revisar cuando
el responsable autorice integrar main:

1. main añade V34 (inspecciones) y actualiza `ShiftSaleHistoryMigrationTest` para
   esperar 33 migraciones y versión 34, en vez de 32/version 33. No se añadió una
   migración en SCRUM-106 ni se cambió esa prueba o una migración anterior.
2. main habilita `@EnableAsync` y republicación de eventos incompletos al reiniciar.
   El cierre y la disponibilidad del historial son eventualmente consistentes:
   una fecha ABSENT/PENDING sigue siendo no disponible. Un GET no debe forzar
   recuperación ni consultar fuentes de ventas alternativas. La configuración
   nueva y el listener de ahorro se dejaron sin integrar.
3. Los cambios de incentivos, inventario, caja del frontend y README permanecen
   en main. No se tocaron sus módulos. Las pruebas de esta entrega corresponden
   al worktree actual, no a un árbol fusionado con main.

## Integración implementada

Consulta de solo lectura para la autoridad exacta `ADMIN`, conforme a la convención
existente (la seguridad por anotaciones de método no está habilitada globalmente):

```http
GET /api/v1/statistics/seller-performance?from=2026-08-03&to=2026-08-03&measure=FUEL_GALLONS&outputScale=2
Authorization: Bearer <token ADMIN>
```

`from`/`to` son fechas operativas inclusivas, con el mismo límite existente de
1..3660 fechas. Un día, semana o mes se expresa como rango explícito; no se inventó
una definición de semana ni de meta global. `measure` acepta `FUEL_GALLONS` o
`BASKET_COP`; `outputScale` es opcional (2), rango 0..12, HALF_EVEN.

`SellerPerformanceService.report` lee una sola vez el rango confirmado de SCRUM-107
y, cuando hay observaciones calculables, una sola vez los parámetros. Reutiliza
`SellerPerformanceCalculator` para devolver juntos `compliance`, `comparison` y
`normalization`, junto con unidad, escala, política de redondeo y
`source=CONFIRMED_SHIFT_SALES_HISTORY`. Esta etiqueta identifica la fuente consultada;
la cobertura y disponibilidad de cada subreporte indican si el rango está completo.
Cada subreporte conserva su rango, métrica, cobertura y motivos de no disponibilidad.
No se filtra el historial por vendedor antes de calcular el promedio de la estación.
La consulta devuelve todos los vendedores observados; no incorpora aún un selector
HTTP de vendedor ni una interfaz gráfica.

El puerto **interno de statistics** `SellerPerformanceParameterSource` recibe rango
y métrica y devuelve listas inmutables de metas individuales, clasificaciones
históricas y multiplicadores explícitos, con la procedencia exigida por los modelos
existentes. Es opcional: sin productor registrado se usan listas vacías, nunca metas
cero ni factores de identidad. No se registró un adaptador ficticio. No es un contrato
compartido nuevo ni un endpoint para cargar ventas o parámetros.

La consulta funciona sin ese productor: ventas confirmadas quedan visibles en las
filas de cumplimiento, con `target`/`percent` nulos y `TARGET_NOT_PROVIDED`.
Comparación/normalización quedan `UNAVAILABLE` con
`SHIFT_CLASSIFICATION_NOT_PROVIDED`. Con clasificación verificada pero sin factor,
normalización indica `MULTIPLIER_NOT_PROVIDED`; no impide otros cálculos verificables.

Los rangos incompletos no producen totales parciales; los confirmados vacíos devuelven
`NO_DATA`. No se consultan parámetros para rangos incompletos, vacíos o con ajustes
negativos sin política aprobada. Se conserva la diferencia entre una venta cero y
una fecha ausente. Las fuentes fallidas, nulas, corruptas o ambiguas generan 502
`PERFORMANCE_SOURCE_FAILURE`, sin exponer SQL/documentos ni convertir el fallo en
un 200 vacío. Solicitudes inválidas generan 400; usuarios sin ADMIN, 403. La cadena
de seguridad existente también rechaza solicitudes anónimas. No hay escrituras,
reprocesamiento, nuevos jobs ni transacciones de registro desde esta consulta.

## Contratos públicos revisados en HEAD y origin/main

- `sales` expone SellerCloseQuery, JournalClosedEvent, CashBalanceQuery y
  CashBalanceTableInfo; también están declarados BasketSalesReadPort y
  WorkedShiftsReadPort. Ninguno entrega metas individuales asignadas por UUID,
  período exacto, unidad y versión/procedencia, ni clasificación histórica DAY/NIGHT.
- SalesTargetService, SalesDistributionService, SalesTargetDistributionEngine,
  SalesJourneyStatistics, StatisticsSeries y ExactFraction siguen en paquetes
  **internos de sales**. No se importaron, copiaron ni invocaron sus endpoints desde
  statistics. El motor local de rendimiento existente es el reutilizado aquí.
- `shifts::catalog` expone `ShiftCodeInfo.period`, pero solo en el catálogo activo
  actual: no certifica el período histórico de una venta cerrada.
- `shifts::daystaff` expone fecha operativa, código/rol/segmentos de los miembros y
  novedades, sin snapshot histórico DAY/NIGHT ni clasificación de festivo. Una malla
  o unos segmentos actuales no sustituyen un contrato histórico aprobado.
- SCRUM-107 conserva código y rol efectivos y ventas originales, pero no período
  DAY/NIGHT ni tipo de día histórico. Los puertos de asistencia/productos de sales
  tampoco pueden implementarse suponiendo que historial agregado equivale a
  asistencia completa o a detalle por producto.

No apareció en main un contrato público que permita completar estos adaptadores
respetando Spring Modulith y las restricciones de propiedad de los módulos.

## Contraste directo con los Excel originales

Se leyeron, sin modificarlos ni recalcularlos, los XML de los originales en
`../repo/docs/references` (ruta relativa a la raíz del worktree). No se instalaron
sus valores como configuración o ventas de producción. SHA-256:

- `Historias Usuario Estacion.xlsx`:
  `7f8f5626a8194549ed28de8023f0b1a83b1e7d974f78be12aac1f83915b40245`.
- `6 OBJETIVOS VENDEDORES BELLAVISTA AGOSTO 2026 (2).xlsx`:
  `3a4bbd81e2254dd61fedc73a568ddfed29c879bd53997e437ef72ae9cabd7555`.

| Evidencia original | Regla verificable / decisión |
| --- | --- |
| Historias, Estadísticas y Ventas, J12:J13 | HU-15: porcentaje individual descendente; incluir cierres sin carga adicional. El GET consume el historial ya registrado, pero no completa metas inexistentes. |
| MODELO NUEVO, AL14 = AH14/AJ14, AL29 = AH29/AJ29 | Cumplimiento contra meta individual, no contra la meta de toda la estación. El porcentaje se presenta multiplicado por 100. |
| MODELO NUEVO, AK14 = AH14/$B$6 | Contribución a meta de estación, una métrica distinta que no reemplaza AL14. |
| CALC METAS, C4 = B4/SUM($B$4:$B$7), D4/E4 = C4 por meta mensual | La meta individual procede del reparto calculado. No se reconstruye en statistics ni se divide una meta mensual entre cuatro o entre días. |
| MODELO NUEVO, B7 = B6/4 y B8 = B6/B4 | Son divisiones presentes en esa hoja; no prueban una política general para semanas parciales o metas diarias individuales. |
| Historias, J19:J20 | HU-18 requiere tipo de turno/día y promedio de estación; la comparación necesita clasificación histórica y población de referencia explícitas. |
| ESCENARIOS, B3:B18 | AVERAGEIF de F por escenario H: promedios observados por día/turno; incluye ceros numéricos. A1 los rotula sin apoyo. No equivale automáticamente a promediar ventas diarias totales. |
| BASE DE DATOS, H2 | Festivos tienen escenario propio; no se sustituye silenciosamente por el día de semana. |
| BASE DE DATOS, I2 = F2/VLOOKUP(H2,ESCENARIOS!A:B,2,FALSE()) | índice adimensional observación/base, no un multiplicador nocturno universal ni una venta normalizada en galones. |
| BASE DE DATOS, J2 y ESCENARIOS, E2:E9 | índice/promedio diario distinto; las fórmulas diarias excluyen ceros. Esa exclusión no se aplica a todo promedio de turnos. |
| BASE DE DATOS, AC2 = -2.143 | Existen ajustes negativos en el original. Hace falta decidir su asignación antes de usarlos en rendimiento. |
| PARAMETROS, A71:B72 y CALC METAS, T11:W11 | 70/30 es reparto TITULAR/APOYO; no es factor DAY/NIGHT. El 50/50 de referencia tampoco. |
| Historias, J22 | HU-19 exige un factor configurado y turno visible; no especifica valor, operación, vigencia o mezcla de métricas. No se habilita normalización de producción sin esa regla. |

El promedio local conserva cohortes por día de semana, tipo de día, DAY/NIGHT,
código exacto y rol efectivo. Es una comparación conservadora de cierres observados,
con población rotulada e incluyendo al vendedor; no se afirma que reproduzca toda
la población/ventana histórica ni cada índice de BASE DE DATOS. El motor previo de
HU-19 solo admite multiplicadores explícitos aprobados. No se deriva `1/base` del
Excel ni se presenta un índice adimensional como venta normalizada.

## Dependencias y orden recomendado para completar SCRUM-106

1. Acordar con sales un contrato público de metas individuales asignadas (UUID,
   ventana operativa exacta, métrica/unidad, importe exacto, versión y procedencia).
   Definir días/semanas parciales, período global y alineación del reparto de horas
   de Excel con fechas operativas nocturnas antes de habilitar cumplimiento real.
2. Acordar con shifts/sales un snapshot histórico DAY/NIGHT y tipo de día por cierre,
   con procedencia estable. No usar nombre/código ni catálogo actual como inferencia.
3. Configurar y versionar la regla de normalización: multiplicación, división o índice,
   unidades de salida, vigencia, población/base, festivos, apoyos, ceros y ajustes.
   El puerto no resuelve esas decisiones de negocio.
4. Resolver en el módulo propietario la incompatibilidad aún presente en main entre
   UUID de 36 caracteres y `vendor_document VARCHAR(30)` de sales (V22 y entidad).
   Statistics no cambia esa migración ni el productor. Se conservan incidentes
   pendientes por documentos/empleados desconocidos, inactivos o turnos no resueltos.
5. Una vez públicos esos contratos, implementar el adaptador interno del puerto,
   conectar el frontend/selector de vendedor y validar extremo a extremo con cierres
   reales. Los escenarios disponibles de pruebas usan fixtures explícitos y no
   constituyen una fuente de producción.
6. Tras integrar main de forma autorizada, repetir Modulith, migraciones hasta V34
   y eventos/cierre con la configuración asíncrona y el listener de ahorro nuevos.

HU-15, HU-18 y HU-19 no se declaran terminadas: queda integrado el recorrido
HTTP → servicio → historial → motores, con dependencias ausentes explícitas.

## Verificación

Las pruebas nuevas ejercitan HTTP real con JWT y guardia ADMIN, servicio y motores
reales con puertos simulados, validación de fechas/rangos/unidad/escala, orden y
fracciones exactas, procedencia, fuentes faltantes/fallidas/corruptas, lectura única
y ausencia de escrituras. La prueba PostgreSQL existente del incremento anterior
ahora también verifica el reporte automático sin productor opcional en un contexto
Spring real. Todas las bases usadas son contenedores efímeros, sin cargar datasource
de desarrollo. Resultados finales de esta entrega:


- Selección focalizada: **75 pruebas**, 0 fallos, 0 errores, 0 omitidas.
  Incluye 14 HTTP/JWT, 7 de integración del servicio, 10 de persistencia,
  los motores/modelos previos y la verificación Spring Modulith.
- Selección final ampliada: **322 pruebas en 30 clases**, 0 fallos, 0 errores,
  0 omitidas; **BUILD SUCCESS**, 1 min 23 s. Incluye las 75 anteriores, no se
  suman como pruebas distintas. Se cubren cierre y eventos de jornada, historial,
  recuperación, concurrencia, migraciones reales hasta V33 de esta rama,
  motores de reparto/metas y promedios, canasta y límites de API/Modulith.
- `git diff --check`: sin errores. HEAD y rama se conservan; los dos archivos
  versionados ya modificados antes de este turno mantienen sus mismas 37 líneas
  añadidas. El resto sigue local, sin staging/commit/push.
- La primera compilación detectó una importación estática ambigua en una
  prueba nueva; se corrigió antes de las dos ejecuciones aprobadas.

Comandos ejecutados desde `repo-scrum106/backend`:

```powershell
mvn -o '-Dtest=SellerPerformance*Test,ShiftSaleHistoryRangeTest,SalesStatisticsModularityTest' -l target/scrum106-focused.log test

mvn -o '-Dtest=ClosedJournalSnapshotTest,ShiftSale*Test,JournalSalesProcessingTest,SalesStatisticsModularityTest,JournalServiceTest,JournalClosedEventListenerTest,JournalClosingInventoryListenerTest,SalesPublicApiBoundaryTest,SellerPerformance*Test,SalesTargetDistributionEngineTest,SalesTargetJourneyStatisticsTest,SalesTargetHistoricalDayAveragesTest,SalesTargetStoragePolicyTest,SalesTargetMaximumSaleTest,SalesTargetPublicShiftCoverageResolverTest,BasketStatisticsServiceTest' -l target/scrum106-regressions.log test
```

Los logs y reportes XML están en `backend/target` (ignorado por Git). Las
pruebas usan PostgreSQL 16 efímero y mocks; no se cargó el datasource de
la base de desarrollo. No se afirma haber ejecutado toda la suite del repositorio
ni las pruebas del main no integrado. Las advertencias de Flyway sobre PostgreSQL
16 y del agente Java no produjeron fallos.
