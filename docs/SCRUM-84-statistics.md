# SCRUM-84 · Estadísticas en Angular

Revisión: 23 de octubre de 2026. Implementación frontend en `feature/front/statistics-scrum84`, con HEAD conservado en `8f88b3800bfeed649e78b1ec36505d4a04cd4046`.

La pantalla está disponible en **Resultados → Estadísticas**, `/resultados/estadisticas`. Reutiliza la entrada existente del menú y contiene cuatro pestañas. El frontend está implementado; la disponibilidad de ventas históricas y asistencias sigue dependiendo de fuentes que todavía no existen en producción. No se consideran completas esas estadísticas por haber construido su interfaz.

## Referencia visual inspeccionada

Se leyeron y renderizaron los originales de `docs/references/prototipo/css/`: `index.html`, `js/views/promedios.js`, `escenarios.js`, `canastas.js`, `competencia.js`, `graficas.js`, `balance.js`, sus utilidades de navegación y las hojas de estilos. Las siguientes nueve capturas del prototipo se abrieron visualmente:

| Captura original | Pantalla comprobada |
| --- | --- |
| `promedios.png` | Contenedor Estadísticas, indicadores, pestañas, fechas y desglose diario |
| `escenarios.png` | Tarjetas A/B azul y naranja, horarios, VS y Comparar |
| `canastas.png` | Filtro mensual, vendedor/documento/avatar, turnos, ventas, promedio, participación y total |
| `competencia.png` | Filtros y matriz de cuatro combustibles con promedio de zona |
| `competencia-simulador.png` | Desplegable y simulador de dos columnas |
| `visor-graficas.png` | Diálogo, leyenda, barras, indicadores, descarga y retorno |
| `visor-lineas.png` | Selector y visor de líneas |
| `exportar-balance.png` | Selector mensual y cuatro miniaturas de diapositivas |
| `promedios-movil.png` | Comportamiento original a 390 px; el menú fijo dejaba el contenido casi oculto |

Se confirmó visualmente **menú lateral izquierdo → Resultados → Estadísticas**. No se ubicó dentro de Metas ni se crearon cuatro rutas independientes.

## Alcance y disponibilidad

| Sección | Implementación | Fuente y límites |
| --- | --- | --- |
| Promedios por turno | Fechas inclusivas, tabla del prototipo, etiquetas día/noche, diferencias y porcentajes con negativos rojos, consulta al motor existente | `CLIENT_SUPPLIED_OBSERVATIONS`. Las observaciones son totales por fecha/jornada, sin vendedor ni asistencia. La columna vendedor muestra `—`. Una observación no acredita un turno trabajado. |
| Escenarios | A/B, horarios 06:00–18:00 y 18:00–06:00 del prototipo, día de semana, VS, Comparar y resultados independientes | Promedios del motor de escenarios explícitos de SCRUM-40. No son una predicción estocástica ni ventas realizadas. Los horarios son referencias visuales; el cálculo recibe `DAY`/`NIGHT`. |
| Lubricantes y otros | Mes, Consultar, Reajustar, tabla completa, agrupación por vendedor, detalle de categorías/cantidades, total, barras de participación | Contrato SCRUM-83. Solo se muestran vendedores con grupos de ventas reales. Documentos enriquecidos con `WorkersService`; su fallo no elimina los totales. Promedio/turno únicamente con asistencia disponible. |
| Competencia | Filtros por combustible y marca, matriz, precio vigente Bellavista, promedio de zona, registro manual de estación y precio, simulador desplegable | Contrato del PR de estadísticas por período. Diferencia competitiva y margen bruto estimado son campos diferentes. Sin volumen real no se calculan ventas ni margen diario. |
| Gráficas | Visor común de barras/líneas, leyendas, valores exactos, unidades, indicadores, retorno y PNG 1800×640 | Observaciones explícitas, series reales de canastilla o sondeos registrados, según pestaña. No se generan puntos faltantes ni eficiencias supuestas. |
| Balance | Diálogo, consulta del mes seleccionado, cuatro miniaturas y exportación PPTX con cuatro diapositivas | Resumen de observaciones, comparación día/noche por backend, ranking solo con ventas reales y disponibilidad por fuente. Se explicita la falta de cuotas, metas, asistencias y recomendaciones fundamentadas. |

El editor **Datos del análisis** es una interacción secundaria en un diálogo. No sustituye el filtro de fechas ni modifica la estructura de la tabla. Sus observaciones son transitorias: no hay persistencia en servidor ni conversión automática a ventas históricas.

`UNAVAILABLE` significa fuente no disponible; `NO_DATA` significa consulta válida sin registros; `AVAILABLE` permite presentar sus datos. Los dos primeros mantienen resumen nulo, tabla vacía y `—` en totales. Un cero explícito sí es un dato; un dato ausente no se convierte en cero. La disponibilidad de turnos trabajados es independiente de las ventas. No se cuentan asignaciones programadas como asistencia.

## Contratos consultados y endpoints conectados

Se usaron `git show` y `git ls-tree` en modo de lectura, sin incorporar estos PR:

- `origin/feature/back/period-statistics-scrum83`: `44d756df1639129ce881fbc4cf001dce0e8ac6c5`.
- `origin/feature/back/sales-statistics-scrum83`: `d9a447b79cf619a7594d5d939ba2a98301c55467`.

Se inspeccionaron los DTO/controladores y modelos públicos reales, incluidos `BasketStatisticsResponse`, `StatisticsChartResponse`, `CompetitionRequests`, `MarketPriceService.Comparison/Simulation` y los contratos existentes de promedios y escenarios. Las interfaces frontend consumen la parte pertinente del contrato; importes e identificadores Long se decodifican como texto exacto.

Base HTTP: `http://localhost:8080`, siguiendo los servicios existentes. Se reutilizan `authInterceptor`, `errorInterceptor`, el guard de autenticación, `AuthService`, `WorkersService`, `AvatarComponent`, `EmptyStateComponent`, `FormFieldComponent`, los estilos/tokens y los validadores decimales existentes.

| Método | Endpoint | Uso |
| --- | --- | --- |
| POST | `/api/v1/sales/averages/explicit` | Promedios y comparación día/noche del balance |
| POST | `/api/v1/sales/scenarios/compare/explicit` | Comparar escenarios |
| GET | `/api/v1/sales/statistics/basket?from=YYYY-MM-DD&to=YYYY-MM-DD&period=DAY&outputScale=2` | Tabla, detalle, series reales y balance mensual |
| GET | `/api/workers` | Nombre/documento de vendedores presentes en las ventas |
| GET | `/api/v1/competition/competitors` | Directorio de estaciones y filtros |
| POST | `/api/v1/competition/competitors` | Registro manual de estación |
| POST | `/api/v1/competition/observations` | Registrar sondeo con precio y fecha/hora |
| GET | `/api/v1/competition/comparison` | Matriz y referencias del mercado |
| POST | `/api/v1/competition/simulation` | Diferencias competitivas y margen con costo opcional |

La navegación usa el guard existente basado en sesión. La autorización **ADMIN** corresponde a los endpoints del backend; el frontend conserva JWT y presenta los errores de autorización. No se afirma una comprobación de roles adicional en el guard ni una prueba de seguridad contra un servidor real.

Ejemplos de estructura para introducir datos explícitos, no registros precargados:

```json
{
  "from": "2026-08-01", "to": "2026-08-31",
  "category": "FUEL_AGGREGATE", "outputScale": 2,
  "observations": [{"date": "2026-08-03", "journey": "DAY", "value": 0}]
}
```

Comparación: `{ "sales": <solicitud anterior>, "first": {"dayOfWeek":"FRIDAY","journey":"DAY"}, "second": {"dayOfWeek":"SUNDAY","journey":"NIGHT"} }`.

Registro de estación: `{ "name": "<nombre verificado>", "brand": "<marca>", "location": "<ubicación>" }`. Registro de precio: `{ "competitorId": 1, "fuelType": "MOTOR", "pricePerGallon": 16330.25, "observedAt": "2026-08-03T15:30:00Z" }`. El identificador debe corresponder a una estación realmente registrada. La hora ingresada se interpreta en Colombia (`UTC−05:00`) y se envía como instante UTC.

Simulación: `{ "fuelType": "MOTOR", "proposedPrice": 16330.25, "acquisitionCost": null, "asOf": "<instante de comparación recibido>" }`. El costo opcional acepta cero. Sin costo, `estimatedMargin` conserva sus campos nulos; `versusAverage` y `versusMinimum` pueden estar disponibles sin margen.

No se conectaron endpoints de escritura de ventas, ni se modificaron módulos de backend o HU-37. Competencia dispone de registro manual y consultas; la edición/desactivación de estaciones y la paginación del historial no forman parte de esta pantalla.

## Reglas de cálculo y presentación

- Promedios y escenarios se calculan en backend. Se comprueban procedencia, unidad, categoría, fechas, precisión y disponibilidad de la respuesta.
- Se preservan tokens numéricos JSON antes de decodificar; el dinero no pasa por `Number`. Sumas, diferencias, participación y divisiones auxiliares usan texto decimal y `BigInt`. `Number` se limita a conteos seguros, fechas y coordenadas gráficas acotadas.
- Precisión de entrada de observaciones: hasta 18 enteros y 12 decimales, reutilizando el validador existente; precio: positivo, menor que `10^10`, hasta dos decimales; costo: mismo límite, permitiendo cero. Sin separadores de miles; la coma decimal se normaliza.
- Derivados auxiliares: dos decimales, `HALF_EVEN`. Los valores devueltos por backend se conservan con su precisión; el formato colombiano cambia separadores y no redondea importes recibidos.
- Participación por vendedor: suma de sus categorías / total del período × 100. Denominador cero produce `null`. Los turnos no se suman repetidamente por cada categoría.
- Promedio por turno: importe / turnos verificados, únicamente con disponibilidad y conteos consistentes. Cero turnos produce `null`.
- Desviación en Promedios: observación − promedio de su jornada; porcentaje = desviación / promedio × 100. Promedio cero deja porcentaje nulo.
- Referencias de competencia: promedio/mínimo calculados por backend con las últimas observaciones de competidores activos, excluyendo Bellavista, sin límite de antigüedad. Filtrar una marca acota las filas, no recalcula el promedio general de zona. El distintivo diferencial usa el combustible prioritario; con Todos se usa corriente.
- Diferencia competitiva = precio propuesto − precio de referencia. Margen bruto = propuesto − costo; porcentaje de margen = margen / propuesto × 100. No incluye impuestos ni gastos operativos. La posición por precio se deriva de los precios observados, con empates compartiendo posición.
- Las líneas usan fechas efectivamente proporcionadas/recibidas. La matriz actual de competencia solo aporta la última observación por estación/combustible: un punto aislado no se presenta como una tendencia histórica completa.
- Los indicadores del visor describen los puntos mostrados. En barras de promedios, su media es la media de esas barras, no un nuevo promedio ponderado de todas las observaciones. Eficiencia permanece indisponible.

## Comparación visual realizada

Se ejecutó Angular en `127.0.0.1:4201` y se renderizó con Chrome a **1600×1200** y **390×844**. Se abrieron visualmente capturas de las cuatro pestañas, el simulador, los formularios, barras, líneas, balance y móvil, comparándolas con las nueve capturas originales. Se corrigieron posiciones de tarjetas, proporciones de filtros, avatar naranja, colores de diferencias, pie del visor, selector del balance y superposición de indicadores móviles.

Se conservaron tarjetas blancas, bordes/sombras, navegación naranja, tabla y total de canastilla, A/B con VS, matriz y desplegable, los dos visores y las cuatro miniaturas. No se afirma equivalencia píxel a píxel.

Evidencia local fuera del repositorio:

- Originales: `%TEMP%\scrum84-prototype-review\` (nueve capturas).
- Angular final: `%TEMP%\scrum84-final-evidence\` (18 capturas de interfaz, PNG exportado, PPTX y `results.json`).
- Automatización reproducible: `frontend/scripts/statistics.visual.mjs`; requiere Node con WebSocket nativo, Chrome y Angular en desarrollo. Se puede indicar `CHROME_PATH`.

```powershell
cd frontend
npm.cmd run start -- --host 127.0.0.1 --port 4201
node scripts/statistics.visual.mjs http://127.0.0.1:4201/resultados/estadisticas "$env:TEMP\scrum84-review-nueva"
```

Usar un directorio de salida nuevo en cada ejecución. **Solo esta prueba** intercepta todas las llamadas a `localhost:8080`, proporciona fixtures y usa un token de prueba. No alcanza un backend real ni registra datos en producción. No hay proveedor de ejemplo en la aplicación. `statistics-test-data.ts` solo se importa desde pruebas y no se usa como fallback.

Diferencias que se mantienen respecto al prototipo:

1. Se conserva el header/menú Angular existente, incluidos Cerrar sesión y sus grupos actuales; no se copian el usuario, la jornada ni las notificaciones simuladas del prototipo. La entrada Estadísticas no se duplicó. La adaptación del menú móvil se limita a esta ruta.
2. Las filas y cifras dependen de datos auténticos. No se copian nombres, documentos, 78 turnos, ocho competidores, porcentajes conciliados ni ventas de las demostraciones originales.
3. Promedios identifica observaciones y muestra vendedor desconocido. Escenarios identifica el promedio observado en lugar de prometer una predicción. Los promedios de asistencia y la eficiencia siguen pendientes de fuentes verificables.
4. Competencia añade registro manual y selección del combustible/costo opcional para el contrato real. No copia el volumen fijo, ventas diarias o recomendaciones comerciales del simulador original.
5. El visor genera sus propias series SVG y tooltips nativos con los datos disponibles; no conserva las cuatro semanas, el pico de quincena o los resultados fijos del prototipo.
6. El balance conserva las cuatro secciones visuales, pero no calcula cumplimiento de cuotas, avance contra metas o aprovisionamiento sin sus fuentes. Exporta disponibilidad explícita en esas secciones. Se verificó el archivo OOXML, no su renderizado en PowerPoint/LibreOffice.
7. Móvil usa menú plegable, tarjetas apiladas y desplazamiento de tablas/pestañas. Se corrige el fallo de anchura del prototipo para mantener todos los controles accesibles.

## Pruebas y resultados

| Comprobación | Resultado |
| --- | --- |
| `npm.cmd run build` (producción) | Correcta. Estadísticas y PPTX en módulos dinámicos; sin errores ni advertencia de tamaño de estilos de Estadísticas después de separarlos. |
| `npm.cmd test -- --watch=false` | **82 pruebas correctas, 13 archivos**; incluye las 48 pruebas existentes y 34 nuevas. |
| `npm.cmd run test:pwa` | **5/5 correctas** sobre el build de producción. Sin caché de API. |
| ESLint de todos los TS/HTML creados/modificados | Correcto. |
| `npm.cmd run lint` general | Falla por 23 errores preexistentes de journal, login, auth y gauge-table. No se modificaron esos archivos para ampliar el alcance. |
| `statistics.visual.mjs` | Correcta: entrada única de menú, cuatro pestañas, HTTP de escenarios/simulación, formularios manuales, vacíos/nulos, PNG 1800×640, PPTX con cuatro diapositivas y móvil sin desbordamiento ni indicadores superpuestos. Cero excepciones JS. |
| `git diff --check` | Correcto. |

Las pruebas cubren precisión monetaria, calendarios/bisiestos, fechas inclusivas, duplicados, observación cero, divisor cero, procedencia, agrupación por vendedor/categoría, asistencia no duplicada, JWT y estados HTTP 401/403/404/500, formularios, filtros, cancelación de respuestas obsoletas, selección mensual del balance, guard/ruta y cierre de diálogos. Las regresiones existentes de metas, jornadas y autenticación siguen pasando.

Incidencias corregidas durante la verificación: límites de estilos por componente; foco accesible del diálogo; formato nativo de ruta Windows en el controlador de descargas de la prueba; selectores dinámicos del simulador en la automatización; tipografía del PNG al serializar SVG. Las primeras ejecuciones con sandbox fallaron al resolver directorios ascendentes de esbuild; las ejecuciones autorizadas completaron compilación y pruebas.

Advertencias restantes del build: bundle inicial **537,63 kB** frente al aviso de 500 kB, CSS de Configuración existente **5,74 kB** frente a 4 kB y dependencia CommonJS `jszip` de la nueva exportación PPTX. No se aumentaron los límites para ocultarlas.

No se levantó el backend ni se verificaron peticiones contra un servidor con ambos PR integrados. Falta esa prueba de integración con JWT ADMIN real, CORS y las fuentes de ventas/asistencia. Los contratos se comprobaron mediante pruebas HTTP y las referencias remotas locales indicadas.

## Archivos

Modificados:

```text
frontend/package.json
frontend/package-lock.json
frontend/src/app/app.routes.ts
frontend/src/atomic-design/templates/dashboard-layout/dashboard-layout.component.ts
frontend/src/atomic-design/templates/dashboard-layout/dashboard-layout.component.html
```

Creados:

```text
docs/SCRUM-84-statistics.md
frontend/scripts/statistics.visual.mjs
frontend/src/atomic-design/templates/dashboard-layout/dashboard-layout.component.css
frontend/src/core/services/competition.service.ts
frontend/src/core/services/sales-statistics.service.ts
frontend/src/core/services/statistics-http.spec.ts
frontend/src/shared/helpers/sales-statistics.helpers.ts
frontend/src/shared/helpers/sales-statistics.helpers.spec.ts
frontend/src/shared/helpers/statistics-contracts.helpers.ts
frontend/src/shared/models/dtos/sales-statistics.dto.ts
frontend/src/shared/models/dtos/basket-statistics.dto.ts
frontend/src/shared/models/dtos/competition.dto.ts
frontend/src/features/statistics/statistics-workspace.ts
frontend/src/features/statistics/statistics.css
frontend/src/features/statistics/statistics-visuals.css
frontend/src/features/statistics/statistics-balance.ts
frontend/src/features/statistics/statistics-test-data.ts
frontend/src/features/statistics/statistics-components.spec.ts
frontend/src/features/statistics/pages/statistics-page.component.ts
frontend/src/features/statistics/pages/statistics-page.component.html
frontend/src/features/statistics/components/shift-averages.component.ts
frontend/src/features/statistics/components/shift-averages.component.html
frontend/src/features/statistics/components/scenarios.component.ts
frontend/src/features/statistics/components/scenarios.component.html
frontend/src/features/statistics/components/scenarios.component.css
frontend/src/features/statistics/components/basket-statistics.component.ts
frontend/src/features/statistics/components/basket-statistics.component.html
frontend/src/features/statistics/components/competition-statistics.component.ts
frontend/src/features/statistics/components/competition-statistics.component.html
frontend/src/features/statistics/components/competition-statistics.component.css
frontend/src/features/statistics/components/observations-editor.component.ts
frontend/src/features/statistics/components/observations-editor.component.html
frontend/src/features/statistics/components/observations-editor.component.css
frontend/src/features/statistics/components/statistics-dialog.component.ts
frontend/src/features/statistics/components/statistics-chart.component.ts
frontend/src/features/statistics/components/statistics-chart.component.html
frontend/src/features/statistics/components/statistics-balance.component.ts
frontend/src/features/statistics/components/statistics-balance.component.html
```

No se modificaron backend, Flyway, Docker, credenciales ni los archivos de `docs/references/`. No se alteraron `gasstation_db` ni `gasstation_db_v2`. No se ejecutaron `git add`, `commit`, `push`, `merge`, `rebase` ni cambios de rama. Todos los cambios de implementación permanecen sin preparar en Git.
