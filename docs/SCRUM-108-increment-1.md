# SCRUM-108 · Incremento 1: Cumplimiento (C-14 / HU-15)

Implementado sobre `/resultados/metas`, en `AnnualGoalsPageComponent`. No se añade
otra página ni se modifica la ruta. Se conservan cabecera, pestañas, controles,
columnas, acordeón y gestión de Metas. Se reutilizan SearchField, Avatar,
PercentBar y EmptyState, los tokens existentes y los patrones HTTP de SCRUM-84.
Los cambios de CSS se limitan al contenido de Cumplimiento.

## Referencias originales

- `docs/references/Historias Usuario Estacion.xlsx`, hoja **Estadísticas y Ventas**,
  filas 12–13: consulta por período, orden descendente e inclusión de cierres sin
  carga adicional.
- Prototipo disponible: `docs/references/prototipo/css/index.html`,
  `js/views/cumplimiento.js` (filtros, tabla, estados vacíos) y
  `js/views/metas.js` (contenedor Metas y cumplimiento).
- Sus estilos: `css/tokens.css`, `css/components.css`, `css/views.css` y el diseño
  ya trasladado a `annual-goals-page.component.css` / `frontend/src/styles.css`.

Se conserva esa estructura visual; no se copian el reparto de demostración,
vendedores, documentos, incentivos, porcentajes ni valores predeterminados del
prototipo. Los estados de cumplimiento se limitan a meta alcanzada/por debajo
de la meta, o no calculable con motivo, sin umbrales de desempeño inventados.

## Contrato y comportamiento

`SellerPerformanceService` consulta exclusivamente:

```http
GET /api/v1/statistics/seller-performance?from=YYYY-MM-DD&to=YYYY-MM-DD&measure=FUEL_GALLONS&outputScale=2
```

Se reutilizan JWT, authInterceptor, errorInterceptor y ApiError. No hay escrituras,
cargas manuales, consulta alternativa de cierres ni recuperación del historial.
La página consulta al abrir y permite repetir con **CONSULTAR**. Los cambios de
período cancelan la petición anterior, limpian resultados/errores y requieren una
nueva consulta. La búsqueda local conserva el orden y posición originales del
backend y no altera el total del período; incluye vendedores históricos sin
filtrarlos por su estado actual en el directorio de trabajadores.

El DTO/parser es una proyección explícita del contrato real: metadatos y
`compliance`. No interpreta `comparison` ni `normalization` en este incremento,
ni condiciona el cumplimiento a la disponibilidad de esos subreportes.
Valida período, unidad, fuente, precisión, cobertura consecutiva, disponibilidad,
identidad, nulos y coherencia de las razones/porcentajes con las fracciones exactas.
Los tokens numéricos se conservan como texto, incluidos exponentes JSON. No se
ordena por porcentajes redondeados ni se reemplaza null por cero. Solo la
coordenada decorativa de PercentBar se convierte a Number, acotada a 0..150;
el porcentaje escrito conserva el valor recibido aunque exceda esa escala.

Los importes de ventas confirmadas permanecen visibles cuando falta una meta.
Una meta de otro período/unidad no se rotula como meta del período seleccionado.
Los indicadores globales de estación no se reconstruyen sumando metas de los
vendedores observados: el reporte no garantiza esa población ni expone un
cumplimiento global. Se mantiene la consulta preexistente de presupuestos
mensuales para el indicador Meta global; el porcentaje de estación sigue sin
datos. El total de galones suma únicamente filas del historial completo recibido.

## Períodos y disponibilidad

- Día: `from = to = fecha operativa`.
- Mes: primer y último día calendario, incluidos años bisiestos. No se recorta
  silenciosamente al día actual: las fechas no confirmadas hacen que el backend
  devuelva historial incompleto, sin totales parciales.
- Semana: inicio y fin elegidos explícitamente. No se presupone lunes/domingo,
  siete días desde una fecha ni prorrateo de metas. Se comunica en la interfaz
  que la definición de semana y semanas parciales está pendiente.
- Meta global: su ventana de cumplimiento aún no está definida; no se añade un
  modo de consulta que la interprete como un año u otro intervalo arbitrario.

`AVAILABLE` muestra cálculos válidos. `PARTIAL` conserva filas calculables y
no calculables, con su motivo; no significa sumar parte del historial.
`UNAVAILABLE` puede conservar ventas sin porcentaje por parámetros faltantes.
`NO_DATA` corresponde a cobertura completa sin cierres. Cobertura `PENDING` y
`ABSENT` se enumera separadamente; `PROCESSED_EMPTY` no se convierte en venta
cero. Meta cero y ajustes negativos sin política tampoco generan porcentajes.
Errores de red/HTTP y respuestas inválidas muestran error y reintento, sin
convertirse en una respuesta vacía exitosa.

## Dependencias e incremento 2

Falta el productor de `SellerPerformanceParameterSource`: metas individuales por
UUID/ventana/métrica con procedencia. El endpoint funciona sin él, mostrando
ventas confirmadas y `TARGET_NOT_PROVIDED`; no significa que HU-15 tenga ya
porcentajes reales en producción. También depende de que SCRUM-107 confirme el
historial: un cierre y su procesamiento pueden ser eventualmente consistentes.
La pantalla permite consultar nuevamente, sin forzar el procesamiento.

Permanecen pendientes las reglas de semana, semanas parciales, meta global y
ajustes negativos. Corresponden a sus propietarios; este incremento no modifica
backend, migraciones, datos, ventas, turnos, incentivos ni el perfil del trabajador
con su servicio ficticio.

Incremento 2: C-09 en `/resultados/rendimiento`, pestañas Frente a la estación y
Ajustado por turno, DTO/parser de los subreportes restantes y selección de
vendedores. Requiere clasificación histórica y factores aprobados. La navegación
desde Jornada cerrada/HU-47 debe aportar la fecha operativa y reutilizar el
reporte; todavía no se cambia ese módulo.

## Verificación

Pruebas de parser, HTTP e interfaz en `seller-performance.helpers.spec.ts`,
`seller-performance.service.spec.ts` y `annual-goals-compliance.spec.ts`.
Las pruebas previas de registro/bloqueo/errores de Metas y de rutas se mantienen,
con un proveedor HTTP de cumplimiento simulado exclusivo de pruebas.
Resultado final del 14 de noviembre de 2026:

| Comprobación | Resultado |
| --- | --- |
| Angular/Vitest: parser, servicio, Cumplimiento y regresiones de Metas/Estadísticas | **114 pruebas en 10 archivos, todas correctas** |
| ESLint de todos los TS/HTML añadidos o modificados | Correcto, sin errores ni advertencias |
| `npm.cmd run lint` general | 156 errores y 1 advertencia en archivos ajenos sin modificar; ningún error de SCRUM-108 |
| `npm.cmd run build` final | Correcto; conserva advertencias de presupuesto de tamaño y CommonJS (`jszip`/`pako`). C-14 sigue siendo carga diferida |
| Revisión Chrome con `scripts/compliance.visual.mjs` | Correcta, nueve capturas a 1600×1200; prototipo original, disponible, parcial, meta faltante, sin datos, pendiente, error, búsqueda y pestaña Metas |
| `git diff --check` y sintaxis del script visual | Correctos |

La revisión visual usa un perfil temporal, JWT exclusivo de prueba y fixtures
interceptados; ningún request llega al backend real ni se permite escribir desde
esa prueba. Se abrieron las capturas del original y Angular para comparar la
estructura; no se afirma equivalencia píxel a píxel ni validación extremo a
extremo contra datos de producción.

Evidencia final fuera del repositorio:
`%TEMP%\scrum108-compliance-final-20261114\`, incluidas las nueve imágenes y
`results.json`. Reproducción desde `frontend`, con un directorio de salida nuevo:

```powershell
npm.cmd run start -- --host 127.0.0.1 --port 4201
node scripts/compliance.visual.mjs http://127.0.0.1:4201/resultados/metas "$env:TEMP\scrum108-nueva-revision"
```

Selección final de pruebas (desde `frontend`):

```powershell
npm.cmd run test -- --watch=false --include='src/shared/helpers/seller-performance.helpers.spec.ts' --include='src/core/services/seller-performance.service.spec.ts' --include='src/features/annual-goals/pages/*.spec.ts' --include='src/core/services/sales-targets.service.spec.ts' --include='src/shared/helpers/sales-target.helpers.spec.ts' --include='src/core/services/statistics-http.spec.ts' --include='src/features/statistics/statistics-components.spec.ts' --include='src/shared/helpers/sales-statistics.helpers.spec.ts'
```

La instalación local no contenía `pdf-lib`/`exceljs`; se restauró con
`npm.cmd ci --ignore-scripts --no-audit --no-fund`, sin cambiar manifiesto ni
lockfile. Angular necesitó ejecución fuera del sandbox por restricciones de
resolución de archivos. La primera ejecución detectó un acceso opcional de
plantilla y después la prueba de envío detectó la falta de enlace FormGroup;
ambos quedaron corregidos antes de la selección final y de las capturas finales.

No se modificaron backend, migraciones, base de datos, rutas ni módulos ajenos.
La sección HTML y la lógica de gestión de Metas permanecen intactas. No se
ejecutaron commits, push, merge, rebase ni staging.
