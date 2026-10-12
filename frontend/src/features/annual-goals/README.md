# C-14 · Metas del año

Ruta protegida: `/resultados/metas`. Usa el layout y CSS existentes, con las pestañas
Cumplimiento/Metas y el acordeón anual cerrado inicialmente. Solo registra combustible
(`FUEL_AGGREGATE/GALLONS`); no registra canastilla ni modifica metas existentes.

## Contrato verificado

Referencia: `origin/feature/back/sales-targets-statistics` en `b2d9321`,
`docs/SCRUM-40-implementation.md` y los controladores/DTOs reales consultados mediante
`git show`. Esa rama no se incorporó al frontend y su API todavía no existe en main.

`POST /api/v1/sales/targets`: year, month null, category, value, unit y maximumSale.
La consulta inicial y posterior al guardado usa `GET /api/v1/sales/targets?year=...`.
El servicio también prepara GET por ID e identidad. El POST exige la autoridad literal
ADMIN, mientras los GET requieren autenticación. La autorización la decide el servidor.
Las cifras y los Long IDs se leen como texto sin pérdida de precisión; el POST envía
tokens numéricos JSON exactos. Los errores textuales Problem Details conservan el
mismo manejo compartido de errores de las otras pantallas.

Un HTTP 403 conserva su estado y comunica el rechazo, sin afirmar que el administrador
carezca de permisos: Spring Security también puede rechazar rutas aún inexistentes en
main. El mensaje distingue las posibles causas (permisos o API no disponible) e indica
que no se ha confirmado cuál aplica. HTTP 401 pide iniciar sesión, HTTP 404 informa de
la API no disponible y los errores de red conservan el mensaje de conexión. Una consulta
fallida nunca habilita el guardado ni se reemplaza por un resultado vacío exitoso.

## Diferencias necesarias frente al prototipo

- Sin datos de demostración, premios, vendedores ficticios ni constantes del Excel.
- El backend acepta cero, exige cantidades no negativas menores de 10^18 y una
  representación exacta a 12 decimales. No exige maximumSale > value. El formulario
  aplica ese contrato; admite punto/coma decimal sin separadores de miles.
- Margen exacto = maximumSale − value, como `metas.js`; no se fija el +20.7% de demo.
- Las equivalencias usan una meta mensual persistida si existe. Su promedio diario es
  presentación redondeada del presupuesto mensual / días calendario del mes, como el
  prototipo. No es una meta individual operativa ni una división anual entre 12 o 365.
- Sin un presupuesto mensual real, esas equivalencias muestran Sin datos. No se
  escala el presupuesto de 2026 usando la meta anual.
- Se conserva la confirmación previa. No existe botón Desbloquear: no hay PUT/PATCH/
  DELETE. Un registro anterior sin maximumSale se consulta sin inventar ese valor.
- Solo se guarda una categoría, por lo que no hay guardado conjunto ni parcial entre
  combustible y canastilla. La fila Estación puede mostrar ambos presupuestos mensuales
  consultados, sin afirmar un 100% de reparto aún no calculado.
- Año y mes de referencia son seleccionables; la fecha inicial procede del calendario
  local, sin fijar agosto de 2026. El breadcrumb se integra en el encabezado existente.

## Dependencias pendientes

`POST /api/v1/sales/targets/{id}/distribution` requiere una meta **mensual**, un
profileId conocido, outputScale 0..12 y asignaciones explícitas con date, workerId,
shiftCode y endDate opcional. GET `/distribution-profiles/{id}` requiere conocer el ID;
no hay listado/perfil activo ni recuperación automática de las asignaciones. No se
puede iniciar un cálculo válido a partir de la meta anual únicamente. La tabla conserva
sus columnas con Sin datos de metas distribuidas. No se crea un perfil desde el Excel.

Cumplimiento y ganancia esperan APIs de ventas consolidadas, metas individuales e
incentivos. Los endpoints de estadísticas con observaciones explícitas no son una API
de recuperación de ventas reales. Los selectores no presentan cifras, barras ni estados
calculados mientras esas fuentes no estén disponibles. El promedio mensual mostrado
no se reutiliza como cumplimiento diario o semanal.
