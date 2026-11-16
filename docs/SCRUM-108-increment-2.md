# SCRUM-108 · Incremento 2: Rendimiento (C-09 / HU-18 / HU-19)

Trabajo en la carpeta principal `repo`, rama `feature/front/scrum-108-performance`.
Se conserva el incremento 1 de Cumplimiento, sin cambiar su página, controles,
gestión de metas ni parser independiente. No se modifican backend, migraciones
o datos. No se realizan commits, push, merge, rebase ni staging.

## Referencias y elementos reutilizados

El prototipo está disponible y se inspeccionó directamente:

- `docs/references/prototipo/css/index.html`, ruta `#/resultados/rendimiento`.
- `js/views/rendimiento.js`: contenedor C-09, pestañas, filtros, indicadores y
  comparación gráfica frente a estación.
- `js/views/comparacion-ajustada.js`: aviso de configuración de solo lectura,
  selección múltiple, tabla y gráfica de venta real/normalizada.
- `js/core/router.js`: navegación de Resultados y acceso al balance existente.
- `js/views/jornada-cerrada.js`, `js/views/jornada-paso-6.js` y `js/views/balance.js`:
  contexto diario, descargas y separación del balance PPTX.
- Estilos originales `css/tokens.css`, `css/layout.css`, `css/components.css`,
  `css/views.css`, ya trasladados a `frontend/src/assets/css/`.

Antes del incremento existían la entrada Rendimiento en el menú, el botón de
Jornada cerrada y el servicio de SCRUM-106, pero no la ruta ni la pantalla C-09.
Se reutilizan Avatar, SearchField, ShiftChip, EmptyState, StatisticsChart,
StatisticsDialog, DailyDownloadsComponent y los servicios/interceptores existentes.
No se duplican componentes de gráficas ni generadores de archivos.

## Implementación

`/resultados/rendimiento` es una ruta protegida y diferida. El menú existente marca
la página activa, el breadcrumb indica Resultados / Rendimiento y la ruta habilita
el menú móvil ya existente. La corrección del encabezado móvil se limita mediante
la clase `performance-layout` a esta pantalla: las acciones ocupan una segunda
fila y no se superponen con el breadcrumb. El resto de rutas conserva su diseño.

La página usa componentes standalone, OnPush, formularios reactivos y Signals.
Los cambios de período/unidad cancelan HTTP y limpian datos, gráficas, errores y
diálogos anteriores. Las consultas admiten día, mes o rango inclusivo explícito.
No se inventan semanas, metas o factores. El mes completo no se recorta
silenciosamente a la fecha actual.

### Frente a la estación

Selecciona vendedor/escenario o busca sobre los resultados ya calculados.
La tabla conserva cada combinación de día de semana, tipo de día, DAY/NIGHT,
código y rol efectivos, mostrando ventas, observaciones, promedio individual,
promedio de estación e índice recibido. `100 %` equivale al promedio de estación;
no se presenta como una diferencia de `+100 %`.

Los tres indicadores requieren una fila seleccionada, para no sumar ni promediar
cohortes incompatibles. La referencia incluye al propio vendedor y los cierres
equivalentes observados dentro del rango, según `baselineDefinition` de SCRUM-106.
Los filtros locales nunca recalculan o reducen esa población de referencia.
Para consultas de un día, la fila también muestra la meta y cumplimiento del
vendedor para el día completo, tomados de `compliance`, sin distribuirlos entre
escenarios. Metas ausentes o de otra unidad/ventana permanecen no calculables.

### Ajustado por turno

Permite selección múltiple de vendedores y búsqueda. Presenta las ventas reales,
el turno histórico, multiplicador aplicable, ventas normalizadas y procedencia.
ShiftChip recibe un color explícito desde el período histórico: no deduce el
turno por el nombre del código o el catálogo actual. Factores de otra métrica o
ventana no se muestran como factores aplicados.

La gráfica compara ventas reales/normalizadas únicamente para filas calculables.
No se aplica `1`, `1.15`, `70/30` ni ningún otro factor por defecto. No se suma un
ranking global entre DAY/NIGHT ni se declara una regla de equidad no aprobada.

### Gráficas

Se reutiliza StatisticsChart de SCRUM-84 con sus exportaciones PNG y el diálogo
existente. Dos entradas opcionales permiten proporcionar la descripción y ocultar
los KPI genéricos de estadísticas en C-09; sus valores predeterminados conservan
el comportamiento anterior de Estadísticas. Se muestran barras por escenario;
el contrato no aporta semanas ni series temporales para fabricar líneas.

## Contrato y disponibilidad

`SellerPerformanceService.readReport()` reutiliza la misma petición GET, JWT y
tratamiento de errores de `read()`. El método anterior conserva la proyección
independiente de Cumplimiento, que no depende de los parámetros de C-09.

```http
GET /api/v1/statistics/seller-performance?from=YYYY-MM-DD&to=YYYY-MM-DD&measure=FUEL_GALLONS&outputScale=2
```

También se admite `BASKET_COP`. El parser ampliado lee `comparison` y
`normalization` del contrato real y valida metadatos, ventana, unidad, cobertura,
escenarios, conteos, población, ratios, promedios, normalización y procedencia.
Los decimales permanecen como texto exacto; las operaciones de validación usan
BigInt y HALF_EVEN, sin sustituir los resultados mostrados del backend.

- AVAILABLE: se muestran valores recibidos válidos.
- PARTIAL: conviven filas calculables y pendientes; no es un total parcial del historial.
- UNAVAILABLE: se muestran los motivos. Las ventas de una fila pueden existir
  aunque falten multiplicadores; no se inventan sus valores normalizados.
- NO_DATA: rango confirmado sin cierres; no equivale a ventas cero.
- PENDING/ABSENT: se detallan fechas pendientes o sin historial confirmado. No
  se presentan totales ni puntos de gráfico para rangos incompletos.
- ZERO_BASELINE: se conservan promedios cero y el índice permanece nulo.
- HTTP/red/contratos inválidos: error visible y reintento, sin éxito vacío ficticio.

## Jornada cerrada

El botón existente navega a:

`/resultados/rendimiento?date=YYYY-MM-DD&returnTo=jornada`

C-09 consulta exactamente `from = to = date`. Una fecha inválida no se reemplaza
silenciosamente por hoy. El regreso usa `/jornada/cerrada?date=fecha-original`,
aunque se hayan cambiado los filtros, conservando el contexto del padre JournalPage.
`returnTo` solo reconoce el valor fijo `jornada`, sin admitir URL arbitrarias.
Los cambios del módulo de ventas se limitan al manejador de ese botón y sus pruebas;
inventario, consulta de cierres y navegación de los otros pasos se conservan.

## Balance y descargas de Javier Solano

El botón **Balance y descargas** abre StatisticsDialog y reutiliza sin cambios
`DailyDownloadsComponent` (`@author javiersolanop777`) y `DailyDownloadService`.
La fecha de los archivos diarios es independiente del período de rendimiento.
En la primera apertura de una consulta diaria se sugiere esa jornada; en mes/rango
se exige elegir una fecha. Después se conserva la fecha elegida, incluso fuera del
período o con filtros de rendimiento inválidos. El diálogo explica la diferencia.
Cambiar de fecha reinicia los mensajes de descarga para no asociar errores de
otra jornada a la nueva selección. No se agregan Excel de varios días.

Los contratos conectados por el componente son:

- `GET /api/v1/sales/downloads/{date}/sales-sheet`: PLANILLA VENTAS.
- `GET /api/v1/sales/downloads/{date}/lubricants-and-misc`: LUBRICANTES Y VARIOS.

Se conservan sus adaptadores Excel, errores y estados de carga. Los otros archivos
del componente permanecen deshabilitados conforme a su integración actual. Esto
no afirma que todo PDF del sistema carezca de backend: el PDF de caja tiene otro
flujo y no se conecta ni se reimplementa aquí.

El balance PPTX de HU-22 sigue en Resultados / Estadísticas, con un enlace explícito.
No existe un contrato para exportar este reporte C-09 completo por período con su
comparación/normalización; no se simula esa descarga ni se cambia el PPTX de SCRUM-84.

## Alcance terminado y dependencias

| Requisito | Estado |
| --- | --- |
| Ruta C-09, dos pestañas, filtros, tablas, gráfica/visor PNG | Implementado |
| Estados de disponibilidad/error, cancelación y ausencia de valores ficticios | Implementado |
| Navegación y regreso con fecha operativa desde Jornada cerrada | Implementado |
| Botón con componente diario de Javier Solano | Implementado para contratos existentes |
| Cumplimiento del incremento 1 y Estadísticas | Conservados, con regresiones |
| Clasificación histórica y multiplicadores de producción | Depende de productores de SCRUM-106 |
| Metas individuales por período y confirmación del historial | Depende de SCRUM-106 / SCRUM-107 |
| Ranking combinado, series semanales y exportación específica de rendimiento | Sin regla/contrato; no se fabrican |

El repositorio no registra aún un adaptador productivo de
`SellerPerformanceParameterSource`. En ese estado, el GET puede traer ventas en
`compliance`, pero C-09 informa `SHIFT_CLASSIFICATION_NOT_PROVIDED` y no calcula
comparaciones o normalización. Con clasificación pero sin factor muestra
`MULTIPLIER_NOT_PROVIDED`. Construir la interfaz no completa esas dependencias.

## Diferencias visuales respecto al prototipo

Se conservan pestañas, tarjetas blancas, tablas, colores naranja/gris/verde,
chips, aviso de solo lectura, filtros y visor. Se mantiene el header/menú Angular
real, no la identidad/notificaciones/jornada simuladas del prototipo.

Las semanas prefijadas se sustituyen por escenarios reales; las columnas de
ranking/turno principal se sustituyen por escenario completo, procedencia y estado.
Los KPI representan promedios comparables y se dejan vacíos si se han seleccionado
varias cohortes. No se copia el factor nocturno fijo ni sus explicaciones de tráfico.
Se añade la tabla de comparación y el selector de unidad para exponer el contrato.
Las tablas móviles usan desplazamiento horizontal; no se comprimen ocho columnas
en la pantalla del teléfono. No se afirma equivalencia píxel a píxel. La reproducción
literal de semanas/ranking/indicadores globales queda condicionada a reglas y datos.

## Verificación

Las pruebas cubren parser ampliado, HTTP del servicio reutilizado, C-09, filtros,
errores, navegación real con RouterTestingHarness, descarga diaria y regresiones de
Cumplimiento/Estadísticas/descargas. Las pruebas de navegador interceptan todo el
backend y usan un perfil temporal: ninguna llamada alcanza una base real, incluido
el POST `/journals/open` que ya utiliza el padre de Jornada al navegar.

Resultados finales del 14 de noviembre de 2026 (America/Bogota):

| Comprobación | Resultado |
| --- | --- |
| Angular/Vitest, selección de ambos incrementos e integraciones | **143 pruebas en 12 archivos, todas correctas** |
| `npm.cmd run build` | Correcto; C-09 en carga diferida. Advertencias de tamaño existentes y CommonJS (`jszip`/`pako`); bundle inicial 588,24 kB |
| ESLint pertinente | Sin errores nuevos. Nuevos archivos, servicio, rutas, TS de layout/Jornada y gráfica compartida correctos; el HTML de layout mantiene 3 errores preexistentes de mouse/foco en líneas 1, 4 y 39 |
| `npm.cmd run lint` general | Mantiene los mismos 156 errores y 1 advertencia previos; no se corrigen módulos ajenos |
| Chrome escritorio 1600×1200 / móvil 390×844 | Correcto; **21 capturas**, navegación con fecha/regreso, dos pestañas, estados, visor, descargas, menú y comprobación de ausencia de desbordamiento/superposición |
| Preservación de C-14 | Hash SHA-256 idéntico antes/después en los seis archivos de página y pruebas de `annual-goals/pages` |
| `git diff --check` / `node --check scripts/performance.visual.mjs` | Correctos |

Se abrieron visualmente las capturas de ambos originales, ambas pestañas Angular,
las pantallas móviles, sus tablas/gráficas y el diálogo de descargas. La revisión
detectó y corrigió el encabezado móvil superpuesto; la corrección no se aplica a
otras rutas. Las diferencias enumeradas arriba permanecen por la semántica del
contrato y la reutilización del visor, no como sustituciones por datos de demo.
Los listados/gráficos móviles requieren desplazamiento horizontal para conservar
su contenido y etiquetas. No queda una superposición conocida en los tamaños
revisados; no se afirma validación de todos los dispositivos ni igualdad píxel a píxel.

Evidencia final fuera del repositorio:
`%TEMP%\scrum108-performance-final-20261114\` (21 PNG y `results.json`).
Reproducción desde `frontend`, indicando un directorio de salida nuevo:

```powershell
npm.cmd run start -- --host 127.0.0.1 --port 4201
node scripts/performance.visual.mjs http://127.0.0.1:4201 "$env:TEMP\scrum108-performance-nueva-revision"
```

Selección final de pruebas:

```powershell
npm.cmd run test -- --watch=false --include='src/features/performance/pages/*.spec.ts' --include='src/shared/helpers/seller-performance*.spec.ts' --include='src/core/services/seller-performance.service.spec.ts' --include='src/features/sales/pages/journal-closed-page/*.spec.ts' --include='src/features/annual-goals/pages/*.spec.ts' --include='src/features/statistics/statistics-components.spec.ts' --include='src/atomic-design/organisms/daily-downloads/*.spec.ts' --include='src/core/services/daily-download.service.spec.ts'
```

Las pruebas HTTP usan HttpTestingController; la navegación usa el router Angular
real con servicios simulados. La revisión de descargas comprueba fecha y errores,
mientras las pruebas existentes de Javier cubren generación/adaptación del archivo.
No se certifica disponibilidad de parámetros productivos ni aceptación extremo a
extremo con una instancia backend real.

La revisión final posterior, sus correcciones y la regresión completa se registran
en [SCRUM-108-final-review.md](SCRUM-108-final-review.md). Sus resultados sustituyen
la validación parcial de 143 pruebas indicada para el cierre del incremento 2.
