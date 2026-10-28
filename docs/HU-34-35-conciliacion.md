# HU-34 Conciliación Credibanco · HU-42 CSV de Redeban (QR) · HU-35 Puntos Colombia (módulo `sales`, backend)

Migración: **V25** (`V25__create_card_reconciliation.sql`). Verifica con el equipo que siga libre.

## Endpoints (todos bajo `/api/v1/sales`; las escrituras exigen ADMIN)

| Método y ruta | Qué hace |
|---|---|
| `PUT /reconciliation/credibanco/{date}/vendors/{workerId}` | Registra comprobantes (Mastercard, Visa, Amex, y sus columnas de madrugada) y lo declarado en la planilla (Credibanco, QR, redenciones) de un vendedor. Devuelve el día recalculado. |
| `DELETE /reconciliation/credibanco/{date}/vendors/{workerId}` | Quita al vendedor del día. |
| `GET /reconciliation/credibanco/{date}` | Resultado del día: "Cuadra" y diferencia exacta por vendedor, TOTAL EDS, reporte contable, acumulado del mes, mes anterior y comisión. |
| `PUT /reconciliation/credibanco/previous-month/{yyyy-MM}` | Valores iniciales de un mes (fila "VALORES MES ANTERIOR"), usados mientras ese mes no tenga datos diarios. |
| `POST /reconciliation/qr/{date}/redeban` (multipart `file`) | HU-42: importa el CSV de Redeban, suma solo las transacciones **QR aprobadas** del día operativo y asigna cada una al vendedor cuyo turno cubre la hora. Responde cuántas filas aceptó y cuántas ignoró (no QR, no aprobadas, otro día). |
| `GET /reconciliation/qr/{date}/summary/{workerId}` | HU-42 #4: texto para WhatsApp con fecha, vendedor, turno, total QR y estado de cuadre. La imagen la dibuja el cliente. |
| `GET /reconciliation/qr/{date}` | QR del día: Redeban contra planillas, por día y por vendedor, con la lista de transacciones **sin asignar** y su motivo (ambigua, sin hora, sin turno). |
| `PUT /reconciliation/points/{date}` · `GET` | Total de Puntos Colombia del día y su comparación con las redenciones de las planillas. |
| `GET/PUT /parameters/{daily-savings\|credibanco-commission-rate}` · `GET .../history` | Configuración propia de Ventas con fecha de vigencia. |

## Reglas (tomadas de los Excel del cliente)

* **TOTAL EDS = Mastercard + Visa.** No incluye Amex ni QR.
* **Cuadra por vendedor:** Mastercard + Visa + Amex (más madrugada) = Credibanco de su planilla. La diferencia es `comprobantes − planilla`. Nunca bloquea.
* **El QR solo se suma al total contable si todos los vendedores cuadran.**
* **Reporte contable:** franquicia + madrugada del día anterior − madrugada del día.
* **Comisión:** `comisión × TOTAL EDS acumulado del mes`, redondeo HALF_UP a 2 decimales. El QR no paga comisión.
* **Parámetros:** la comisión se guarda como fracción (`0.0265`). Valores iniciales: ahorro $250.000 y 2,65 %.
* **Puntos:** si todas las redenciones son 0 el paso queda `COMPLETED_ZERO` sin digitar nada; si no, se compara el total del día (digitado) con la suma de las planillas.

## Supuestos a confirmar con el cliente

1. **Formato real del CSV de Redeban:** el lector es tolerante (detecta separador, acepta sinónimos de columnas) pero se escribió sin un archivo de muestra. Si el archivo no trae la columna del valor responde *"El archivo no tiene el formato esperado"* y no lo procesa (HU-42 #5). "Aprobada" se reconoce por el texto de una columna de estado (aprobada, approved, exitosa, autorizada, OK, 00); sin columna de estado, todas las filas se toman como aprobadas.
2. **Filas con hora cubierta por dos vendedores** (titular + apoyo) no se atribuyen: quedan `AMBIGUOUS`. Si el CSV trae terminal o caja, ese dato resolvería el caso.
3. **Día operativo de 06:00 a 06:00** (según el documento de flujo): una fila del CSV entre las 00:00 y las 05:59 pertenece al día operativo anterior. Una fila de las 02:30 del 06/08 se concilia en el 05/08, con el turno NOCHE del 05/08.
4. **TOTAL EDS sin Amex:** se sigue la fórmula del Excel (Mastercard + Visa). Es una pregunta abierta del documento de flujo; si el cliente decide incluir Amex, es un cambio de una línea en `DayFranchiseTotals.totalEds()`.
5. La planilla de cobro mensual a Primax (HU-35 escenario 3) **no está incluida**: falta saber de dónde sale el número de transacción.

---

## Probarlo paso a paso con el 05/08/2026

Prerrequisitos: copiar los archivos al repo, confirmar que **V25** está libre y levantar el backend como indica el README. Las rutas de abajo usan `localhost:8080`; también puedes hacerlo desde Swagger.

**0. Pruebas automáticas (no necesitan Docker).** Desde `backend/`:
`mvn test -Dtest='CredibancoReconciliationCalculatorTest,RedebanCsvParserTest,QrAndPointsReconciliationTest,QrWhatsappSummaryTest'`. Después, `mvn verify` completo (este sí usa Testcontainers y valida la migración V25 contra PostgreSQL).

**1. Token.**
```bash
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .token)
H="Authorization: Bearer $TOKEN"
```

**2. Roles y turnos.** `curl -H "$H" localhost:8080/api/shifts/catalog` → anota el `role.id` de TITULAR y de APOYO.

**3. Crear los 4 vendedores** (Ana, Carlos y Wiltom titulares; Mónica apoyo) y anotar cada `id`:
```bash
curl -X POST localhost:8080/api/workers -H "$H" -H 'Content-Type: application/json' \
  -d '{"fullName":"Carlos Andrés Paredes","document":"1000000002","roleId":"<ID_TITULAR>"}'
```

**4. Parámetros de Ventas.** Deben venir sembrados por la migración:
```bash
curl -H "$H" "localhost:8080/api/v1/sales/parameters/credibanco-commission-rate?date=2026-08-05"   # 0.026500
curl -H "$H" "localhost:8080/api/v1/sales/parameters/daily-savings?date=2026-08-05"               # 250000
```

**5. Valores del mes anterior** (fila "VALORES MES ANTERIOR" del Excel):
```bash
curl -X PUT localhost:8080/api/v1/sales/reconciliation/credibanco/previous-month/2026-07 -H "$H" \
  -H 'Content-Type: application/json' -d '{"mastercard":70829095,"visa":34166441,"amex":0,"qr":21926590}'
```

**6. Cargar los vendedores del día** (HU-34). Las planillas son las reales; el reparto Mastercard/Visa por vendedor es de ejemplo, lo que importa es que sumen $3.618.384 y $2.072.928:
```bash
D=localhost:8080/api/v1/sales/reconciliation/credibanco/2026-08-05/vendors
curl -X PUT $D/<ID_MONICA> -H "$H" -H 'Content-Type: application/json' -d '{"shiftCode":"12-7","mastercard":800000,"visa":554436,"planillaCredibanco":1354436,"planillaQr":162606}'
curl -X PUT $D/<ID_ANA>    -H "$H" -H 'Content-Type: application/json' -d '{"shiftCode":"DESCANSO"}'
curl -X PUT $D/<ID_CARLOS> -H "$H" -H 'Content-Type: application/json' -d '{"shiftCode":"DIA6","mastercard":1000000,"visa":1019760,"planillaCredibanco":2019760,"planillaQr":377902,"planillaRedemptions":41380}'
curl -X PUT $D/<ID_WILTOM> -H "$H" -H 'Content-Type: application/json' -d '{"shiftCode":"NOCHE","mastercard":1818384,"visa":498732,"planillaCredibanco":2317116,"planillaQr":335500}'
```
Resultado esperado de `GET .../credibanco/2026-08-05` (con solo el día 5 cargado): `allMatch: true`, `totalEds: 5691312`, `franchises.mastercard: 3618384`, `visa: 2072928`, `accounting.total: 6567320`, `previousMonth.totalEds: 104995536`, `commission: 150819.77` (2,65 % de 5.691.312; con los días 1 a 4 cargados también, el acumulado es 21.497.087 y la comisión 569.672,81).

**7. Provocar una discrepancia.** Repite el PUT de Carlos con `"planillaCredibanco":2000000`: `allMatch` pasa a `false`, Carlos sale con `difference: 19760` y el QR deja de sumarse al total contable. No hay error HTTP: no bloquea.

**8. Valor inválido** (HU-34 #5): `"visa":-1` o `"visa":"abc"` devuelven 400 con `errors[0].field = "visa"`.

**9. QR de Redeban** (HU-35) con el CSV de ejemplo de `docs/examples/`:
```bash
curl -X POST localhost:8080/api/v1/sales/reconciliation/qr/2026-08-05/redeban -H "$H" \
  -F file=@docs/examples/redeban-2026-08-05-ejemplo.csv
```
Esperado: `totalRows: 4`, `accepted: 4` y, dentro de `reconciliation`: `redebanTotal: 876008`, `matchesDay: true`, `perVendorReliable: true`, `unassigned: []` y los 4 vendedores con `matches: true` (la fila de las 02:30 del 06/08 queda con Wiltom).
Texto para WhatsApp de Carlos: `curl -H "$H" localhost:8080/api/v1/sales/reconciliation/qr/2026-08-05/summary/<ID_CARLOS>` → `Total QR: $377.902`, `Estado: Cuadra`.

**10. Puntos Colombia.** `GET .../reconciliation/points/2026-08-05` → `PENDING_TOTAL` (Carlos declaró 41.380 de redenciones). Luego:
```bash
curl -X PUT localhost:8080/api/v1/sales/reconciliation/points/2026-08-05 -H "$H" -H 'Content-Type: application/json' -d '{"total":41380}'
```
→ `status: MATCH`. En un día donde ninguna planilla tenga redenciones, el `GET` devuelve `COMPLETED_ZERO` sin digitar nada.
