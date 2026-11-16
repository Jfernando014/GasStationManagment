# SCRUM-108 · Revisión final de frontend

Fecha: 14 de noviembre de 2026 (America/Bogota).
Carpeta principal `repo`, rama `feature/front/scrum-108-performance`.
Base comparada: referencia local `origin/main`, `5d21b53`; HEAD coincide con ella.
Se revisan los cambios de ambos incrementos y los archivos nuevos, sin commits,
staging, push, merge ni rebase. No se modifica backend, migraciones ni datos.

## Dictamen

**El frontend está listo para un Pull Request de integración con las dependencias
indicadas en este informe.** La suite completa y el build pasan; no hay incidencias
nuevas de lint. Esto no permite declarar completadas todas las historias de negocio:
faltan productores de parámetros y contratos/reglas documentados abajo. El lint
general del repositorio sigue fallando por deuda existente en `origin/main`.

No se dispone de conexión a Jira ni de una exportación adicional del ticket. Se
contrastan los criterios completos de las historias relacionadas disponibles en
`docs/references/Historias Usuario Estacion.xlsx`, las instrucciones de SCRUM-108
de esta tarea y el código real de SCRUM-106/107. No se certifican cambios recientes
de criterios que solo existan en Jira ni un flujo contra una instancia productiva.

## Problemas encontrados y correcciones limitadas a C-09

1. **Espaciado acumulado en Ajustado por turno.** La página usa `gap: 14px`, pero
   heredaba otros 20px del aviso de factores y 10px/12px del selector. El selector
   se renderizaba incluso vacío: entre factores y disponibilidad quedaban 70px sin
   contenido. Se eliminan esos márgenes únicamente en el CSS local y se omite el
   selector sin vendedores. Resultado medido: **70 → 14px** sin vendedores; con
   chips, **98 → 56px**, incluidos sus 28px de altura y dos separaciones de 14px.
   Confirmado en escritorio y móvil; no se rediseñan las interfaces.
2. **Fecha diaria restringida al período.** Se retiran `min`/`max`, la validación
   que exigía pertenecer al período y el bloqueo del botón por filtros de rendimiento
   inválidos. La selección diaria se conserva entre aperturas/cambios de período.
   La primera consulta de un día solo ofrece su fecha como sugerencia inicial;
   una consulta mensual no elige automáticamente el primer día. El texto identifica
   la fecha operativa independiente y separa los archivos diarios del balance PPTX.
3. **Actualización y mensajes de descarga.** El componente recibe directamente la
   señal de la fecha y se reinicia para cada fecha distinta. Así no conserva errores
   o estados de otra jornada. Pruebas verifican fechas válidas sucesivas, una fecha
   fuera del período, fecha vacía/inválida y apertura con período inválido.

Solo se cambian la página C-09, sus pruebas y el guion de comprobación visual,
además de esta documentación y la aclaración del incremento 2. En esta revisión
no se modifica C-14, el componente compartido DailyDownloads ni sus servicios.

## Disponibilidad: contraste con el backend

La fuente verificable es `statistics/domain/calculation/SellerPerformanceCalculator.java`.
El frontend conserva los códigos; no sustituye causas distintas por un mismo fallo.

| Respuesta real | Interpretación y presentación |
| --- | --- |
| `HISTORY_INCOMPLETE` | Cobertura `PENDING`/`ABSENT`; reporte `UNAVAILABLE`, sin filas ni totales parciales. Mensaje de historial incompleto y detalle de fechas. |
| `SHIFT_CLASSIFICATION_NOT_PROVIDED` | Falta clasificación histórica DAY/NIGHT/tipo de día para al menos un cierre. C-09 no compara ni normaliza; C-14 puede conservar ventas. Mensaje específico de clasificación histórica. |
| `MULTIPLIER_NOT_PROVIDED` | Hay clasificación, pero no se proporcionó un multiplicador aplicable. Se conservan ventas originales y turno; factor y ventas normalizadas permanecen nulos. Mensaje por fila de factor no disponible. No prueba por sí solo que el factor nunca haya sido configurado en otro sistema. |
| `MULTIPLIER_PERIOD_MISMATCH` / `MULTIPLIER_MEASURE_MISMATCH` | Factor recibido para otra ventana/unidad; no se muestra como aplicado ni se generan puntos normalizados. |
| `INCOMPLETE_CALCULATIONS` | Motivo agregado de filas no calculables; se consulta la causa concreta de cada vendedor. Puede producir `PARTIAL` o `UNAVAILABLE`, sin equivaler a historial parcial. |
| `NO_DATA` / `NO_CONFIRMED_SELLER_CLOSES` | Período confirmado sin cierres; no equivale a ventas cero ni a fuente fallida. |
| `ZERO_BASELINE` / `ZERO_TARGET` | Se conserva el cero real donde corresponde; la división no calculable queda nula. |
| HTTP/red/respuesta incoherente | Error visible y reintento; se limpian datos anteriores. No se convierte en éxito vacío. |

Los mensajes existentes ya distinguían las tres causas principales. Se refuerzan
las pruebas y las comprobaciones visuales, sin cambiar los parsers ni inventar
un motivo adicional. Los importes y porcentajes se validan con decimales exactos;
el orden de C-14 sigue siendo el del backend, incluso en empates redondeados.

## Descargas y «formato por validar»

DailyDownloadsComponent y DailyDownloadService son los existentes de Javier Solano.
El contrato es diario, separado de `seller-performance`:

| Archivo | Estado verificado |
| --- | --- |
| PLANILLA VENTAS | Habilitado para solicitar `GET /api/v1/sales/downloads/{date}/sales-sheet`. |
| LUBRICANTES Y VARIOS | Habilitado para solicitar `GET /api/v1/sales/downloads/{date}/lubricants-and-misc`. |
| Dominus con saldo en caja, VARIACIÓN, COMPLEMENTO DOMINUS | Deshabilitados: este componente no tiene una petición integrada para ellos. No se habilitan por apariencia. El PDF de caja dispone de otro flujo del proyecto, que no se modifica. |

Los dos GET devuelven XLSX y pueden responder 404 cuando no hay cierres para esa
fecha, o error de generación. No existe un manifiesto para consultar existencia
por archivo/fecha antes de solicitarlo: **habilitado significa contrato integrado,
no archivo garantizado para cualquier fecha**. El diálogo lo explica y conserva
los errores reales; no se simulan archivos. Las pruebas de navegador ejercitan
ambas solicitudes y sus 404, y comprueban que los otros tres botones no solicitan
archivos. Las pruebas existentes cubren la adaptación y generación Excel.

«Formato por validar» procede de
`frontend/src/atomic-design/organisms/daily-downloads/daily-downloads.component.html:4`,
ya presente en `origin/main` (último cambio del archivo: `68a2afa`). Su tooltip dice
«Formato exacto de los Excel pendiente de confirmar con el cliente».
El adaptador existente reconstruye el formato del prototipo desde XLSX reales y
deja vacías las columnas que el backend no aporta. No se encontró evidencia de
aprobación del cliente; se conserva el aviso. No describe la disponibilidad del
historial ni valida las reglas de rendimiento. El formato final y algunas columnas
del adaptador siguen pendientes del flujo de descargas original.

El enlace al balance PPTX apunta a Resultados / Estadísticas (SCRUM-84 / HU-22).
No hay un contrato para exportar el reporte completo C-09 por período y no se
presentan los Excel diarios como ese balance.

## Criterios, referencias y límites

Prototipo original inspeccionado: `docs/references/prototipo/css/index.html`, rutas
`#/resultados/metas` y `#/resultados/rendimiento`; archivos `js/views/cumplimiento.js`,
`metas.js`, `rendimiento.js`, `comparacion-ajustada.js`, `jornada-cerrada.js`,
`jornada-paso-6.js`, `balance.js` y `js/core/router.js`. Se contrastan sus estilos
con los de `frontend/src/assets/css/`, ya trasladados al proyecto.

| Criterio completo disponible | Resultado de la revisión |
| --- | --- |
| HU-15, Estadísticas y Ventas filas 12–13: día/semana/mes/global, CONSULTAR, porcentaje individual descendente | Día, mes y rango explícito de semana implementados; conserva orden backend, ventas, metas y estados. Definición de semanas/parciales y período/cumplimiento global siguen pendientes. C-14 conserva su alcance en galones. No se calcula global con una meta mensual o anual incompatible. |
| HU-15: incluir automáticamente el día cerrado | Consume historial confirmado mediante GET, sin cargas manuales ni reprocesamiento frontend. La confirmación/materialización efectiva corresponde a SCRUM-107. |
| HU-18, filas 19–20: vendedor/período, CONSULTAR y gráfica frente a estación equivalente | Filtros, búsqueda, escenarios y gráfica implementados. Población real de cierres equivalentes dentro de la ventana, incluido el vendedor; los filtros locales no alteran la base de estación. |
| HU-18: acceso desde jornada cerrada y comparación con meta diaria | Botón real navega con fecha operativa; GET `from = to = fecha`; regreso conserva fecha original. Muestra meta/cumplimiento del día desde `compliance` cuando existen, sin repartirlos entre escenarios. Admite galones y canastilla COP en C-09. |
| HU-19, fila 22: GENERAR COMPARACIÓN, factor configurado, resultado normalizado y turno | Implementado para datos calculables recibidos, con procedencia y estados. Se requiere productor real de clasificación/factores para obtener resultados de producción. |
| HU-47, Jornada Diaria fila 17: además aditivos, incentivo diario/mensual, descuadre y posición | Se verifica la navegación y el subconjunto meta/promedio; esos indicadores adicionales no forman parte del DTO de SCRUM-106. No se declara HU-47 completa ni se inventa ranking. Inventario y jornada completa conservan sus flujos. |
| Conservación de Metas y Estadísticas | Gestión Metas intacta; StatisticsChart añade opciones con valores predeterminados que conservan SCRUM-84. Regresión completa correcta. |
| Estados/cancelación/autenticación | Pruebas de cancelación de respuestas antiguas, filtros, campos inválidos, autorización, reintento y cuatro disponibilidades correctas. |

No quedan diferencias de maquetación nuevas conocidas en los tamaños comprobados.
Se mantienen las diferencias funcionales ya documentadas respecto a los datos de
demostración: escenarios reales en lugar de semanas fabricadas, promedio por
observación, sin factor nocturno fijo `1.15`, sin ranking combinado y sin turno
principal inferido. Las tablas/gráficas anchas usan desplazamiento horizontal
interno en móvil. No se afirma igualdad píxel a píxel ni cobertura de dispositivos
no revisados.

## Validación final

| Comprobación | Resultado |
| --- | --- |
| `npm.cmd run test -- --watch=false --progress=false` | **399/399 pruebas, 60/60 archivos**, incluidos C-14, C-09, parsers, HTTP, rutas, Jornada, descargas, Excel y Estadísticas. |
| `npm.cmd run test:pwa` | **5/5** correctas con el build de producción final. |
| `npm.cmd run build` | Correcto. Advertencias de presupuestos de tamaño y CommonJS `jszip`/`pako`; bundle inicial 588,24 kB. El CSS compartido ya supera el presupuesto y este incremento añade dos reglas móviles acotadas. No se presenta un build sin advertencias. |
| ESLint sobre archivos nuevos/modificados | 25 TS/HTML contrastados con `origin/main`: **0 errores y 0 advertencias nuevos**. Lint dirigido correcto; HTML del layout conserva tres errores de mouse/foco, idénticos a la base, en líneas 1, 4 y 39. |
| `npm.cmd run lint` | Falla: **156 errores y 1 advertencia** preexistentes; los archivos restantes con incidencias no cambian en el diff. No se corrigen módulos ajenos. |
| Chrome C-09: 1600×1200 y 390×844 | **22 capturas**; prototipo, dos pestañas, estados, visor, fechas independientes, ambos GET diarios, tres botones deshabilitados, menú, navegación/regreso y hueco de 14px. Sin excepciones ni desbordamiento horizontal de página. |
| Chrome C-14: escritorio y original | **9 capturas**; disponible, parcial, meta faltante, vacío, pendiente, error, búsqueda y Metas conservadas. |
| `git diff --check origin/main`, `node --check` de ambos guiones visuales | Correctos. |

Evidencia externa al repositorio:

- `%TEMP%/scrum108-review-before/`: medición previa de 70px/98px.
- `%TEMP%/scrum108-final-review-performance-v2/`: capturas y `results.json` final C-09.
- `%TEMP%/scrum108-final-review-compliance-correct-url/`: capturas y `results.json` C-14.

Las pruebas de navegador interceptan todo el tráfico backend con fixtures exclusivos
de prueba; no leen ni escriben datos reales. El POST de apertura ya existente del
padre Jornada también se intercepta. No se verificó disponibilidad productiva de
parámetros ni una descarga contra datos reales.

## Revisión del diff y preparación del PR

Se revisan los **13 archivos seguidos modificados** y los archivos nuevos mediante
`git ls-files --others --exclude-standard`; `git diff origin/main` por sí solo no
incluye estos últimos. Los cambios de código corresponden a C-14/C-09, DTO/parsers,
servicio HTTP, pruebas, dos guiones visuales y tres documentos de SCRUM-108.
Las extensiones compartidas son mínimas: ruta/menú/breadcrumb/móvil, dos entradas
opcionales de StatisticsChart y el manejador de navegación de Jornada cerrada.

No hay cambios en backend, migraciones, configuración de base de datos,
dependencias npm/lockfile, ni correcciones ajenas. `git diff --cached` está vacío.
Se detecta `docs/references/dominus-tabla-referencia.png` **sin seguimiento y
preexistente**, ajeno a este incremento: se conserva y debe quedar fuera del PR.
Los Excel y el prototipo son referencias locales ignoradas; no se modifican ni
se agregan forzadamente. Capturas, perfiles de Chrome y builds no forman parte del
diff. No se elimina material del usuario ni se prepara el índice.

Para aceptar el flujo completo en producción quedan: productor de metas
individuales con ventana/unidad/procedencia; historial completo SCRUM-107;
clasificación histórica DAY/NIGHT y festivos; multiplicadores aprobados/versionados;
definición de semana/parciales y global; contrato de exportación C-09 y de indicadores
adicionales HU-47. La validación del formato Excel requiere confirmación del cliente.
Estas dependencias deben constar en el PR y en Jira; no bloquean revisar la
integración frontend que informa honestamente su ausencia.
