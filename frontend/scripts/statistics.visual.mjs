// Browser contract fixtures are restricted to this test script. No request reaches a real backend.
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdir, readFile, readdir, writeFile } from 'node:fs/promises';
import { join, resolve } from 'node:path';
import { tmpdir } from 'node:os';
import JSZip from 'jszip';

const url = process.argv[2] ?? 'http://127.0.0.1:4201/resultados/estadisticas';
const output = resolve(process.argv[3] ?? join(tmpdir(), 'scrum84-angular-review'));
await mkdir(output, { recursive: true });
const stations = [1, 2, 3, 4].map(id => ({ id, name: 'Estación de prueba ' + id, brand: id % 2 ? 'Primax' : 'Terpel', location: 'Ubicación de prueba ' + id, active: true }));
const types = ['MOTOR', 'DIESEL', 'EXTRA', 'MAX_PRO'];
const own = [16330, 11930, 20390, 12790];
const prices = stations.flatMap(s => types.map((fuelType, i) => ({ id: s.id * 10 + i, competitorId: s.id, fuelType, pricePerGallon: own[i] - s.id * 10,
  observedAt: '2026-01-01T12:00:00Z', createdAt: '2026-01-01T12:00:00Z' })));
let basketMode = 'AVAILABLE'; const requests = [], exceptions = [];
const ids = [1, 2, 3, 4].map(n => '00000000-0000-0000-0000-' + String(n).padStart(12, '0'));
const people = ids.map((id, i) => ({ id, fullName: 'Vendedor de prueba ' + (i + 1), document: 'TEST-' + (i + 1), roleId: ids[0], roleName: 'TITULAR', dispenser: 1, active: true }));
function market(type) {
  const observations = prices.filter(p => p.fuelType === type).filter(p => !prices.some(other => other.competitorId === p.competitorId && other.fuelType === type && other.observedAt > p.observedAt));
  return { observations, competitorCount: observations.length, average: observations.reduce((sum, p) => sum + p.pricePerGallon, 0) / Math.max(1, observations.length), minimum: observations.length ? Math.min(...observations.map(p => p.pricePerGallon)) : null };
}
const delta = (price, base) => ({ absoluteCopPerGallon: base === null ? null : price - base, percentage: base ? Number(((price - base) / base * 100).toFixed(6)) : null });
function respond(path, method, body, search) {
  if (path.endsWith('/competition/competitors')) {
    if (method === 'POST') { const station = { ...body, id: stations.length + 1, active: true }; stations.push(station); return station; }
    return stations;
  }
  if (path.endsWith('/competition/observations') && method === 'POST') { const observation = { ...body, id: prices.length + 100, createdAt: new Date().toISOString() }; prices.push(observation); return observation; }
  if (path.endsWith('/competition/comparison')) return { asOf: new Date().toISOString(), bellavistaDate: new Date().toISOString().slice(0, 10), mode: 'CURRENT_LATEST', unit: 'COP_PER_GALLON', activeCompetitorsOnly: true,
    fuels: types.map((fuelType, i) => { const m = market(fuelType); return { fuelType, bellavistaPrice: own[i], bellavistaValidFrom: '2026-01-01', competition: { competitorCount: m.competitorCount, average: m.average, minimum: m.minimum }, versusAverage: delta(own[i], m.average), versusMinimum: delta(own[i], m.minimum), observations: m.observations }; }) };
  if (path.endsWith('/competition/simulation')) { const m = market(body.fuelType); return { asOf: body.asOf ?? new Date().toISOString(), fuelType: body.fuelType, unit: 'COP_PER_GALLON', ...body,
    competition: { competitorCount: m.competitorCount, average: m.average, minimum: m.minimum }, versusAverage: delta(body.proposedPrice, m.average), versusMinimum: delta(body.proposedPrice, m.minimum),
    estimatedMargin: { grossCopPerGallon: body.acquisitionCost === null ? null : body.proposedPrice - body.acquisitionCost, grossMarginPercentage: body.acquisitionCost === null ? null : Number(((body.proposedPrice - body.acquisitionCost) / body.proposedPrice * 100).toFixed(6)) }, observations: m.observations }; }
  if (path.endsWith('/workers')) return people;
  if (path.endsWith('/statistics/basket')) {
    const from = search.get('from'), to = search.get('to');
    const amounts = [2742800, 1927200, 1591800, 919100], total = amounts.reduce((a, b) => a + b);
    const groups = basketMode === 'AVAILABLE' ? amounts.map((amount, i) => ({ workerId: ids[i], workerName: people[i].fullName, category: ['LUBRICANTE', 'ADITIVO', 'REFRIGERANTE', 'OTRO'][i], amount, quantity: 10 + i, saleLines: 2, workedShifts: null, averageAmountPerWorkedShift: null, participationPercent: Number((amount / total * 100).toFixed(2)) })) : [];
    return { from, to, workerId: null, category: null, period: 'DAY', moneyUnit: 'COP', quantityUnit: 'PRODUCT_UNITS', workedShiftsAvailability: 'UNAVAILABLE', workedShiftsReason: 'WORKED_SHIFTS_SOURCE_NOT_CONNECTED',
      summary: groups.length ? { amount: total, quantity: 46, saleLines: 8, workedShifts: null, averageAmountPerWorkedShift: null } : null, groups,
      charts: { source: 'REAL_HISTORICAL_SALES', availability: basketMode, reason: basketMode === 'AVAILABLE' ? null : 'TEST_' + basketMode, from, to, outputScale: 2, roundingMode: 'HALF_EVEN', decimalPolicy: 'EXACT_TOTALS_HALF_EVEN_DERIVED_VALUES', series: [] } };
  }
  if (path.endsWith('/averages/explicit') || path.endsWith('/scenarios/compare/explicit')) {
    const sales = body.sales ?? body;
    const scenarios = body.first ? [body.first, body.second] : [{ journey: 'DAY', dayOfWeek: null }, { journey: 'NIGHT', dayOfWeek: null }];
    return { from: sales.from, to: sales.to, category: sales.category, unit: sales.category === 'BASKET' ? 'COP' : 'GALLONS', source: 'CLIENT_SUPPLIED_OBSERVATIONS', outputScale: sales.outputScale, roundingMode: 'HALF_EVEN',
      series: scenarios.map(s => { const selected = sales.observations.filter(o => o.journey === s.journey && (!s.dayOfWeek || ['SUNDAY','MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY'][new Date(o.date + 'T12:00:00Z').getUTCDay()] === s.dayOfWeek));
        const total = selected.reduce((sum, o) => sum + o.value, 0); return { ...s, label: s.journey === 'DAY' ? 'Diurna' : 'Nocturna', total, observations: selected.length, available: selected.length > 0, average: selected.length ? Number((total / selected.length).toFixed(2)) : null, exactAverage: selected.length ? { numerator: String(total), denominator: String(selected.length) } : null }; }) };
  }
  return undefined;
}
const chrome = spawn(process.env.CHROME_PATH ?? 'C:/Program Files/Google/Chrome/Application/chrome.exe', ['--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--disable-background-networking','--disable-extensions','--remote-debugging-port=0','--user-data-dir=' + join(output, 'chrome-profile'), 'about:blank'], { windowsHide: true, stdio: ['ignore','ignore','pipe'] });
let socket;
try {
  const endpoint = await new Promise((resolve, reject) => { let log = ''; const timer = setTimeout(() => reject(new Error('Chrome timeout')), 20000); chrome.stderr.on('data', data => { log += data; const match = log.match(/DevTools listening on (ws:\/\/[^\s]+)/); if (match) { clearTimeout(timer); resolve(match[1]); } }); chrome.on('error', reject); });
  socket = new WebSocket(endpoint); await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
  let next = 0; const pending = new Map();
  const call = (method, params = {}, sessionId) => new Promise((resolve, reject) => { const id = ++next; pending.set(id, { resolve, reject }); socket.send(JSON.stringify({ id, method, params, sessionId })); });
  socket.onmessage = async event => {
    const message = JSON.parse(event.data), task = pending.get(message.id);
    if (task) { pending.delete(message.id); message.error ? task.reject(new Error(JSON.stringify(message.error))) : task.resolve(message.result); }
    if (message.method === 'Runtime.exceptionThrown') exceptions.push(message.params.exceptionDetails);
    if (message.method === 'Browser.downloadProgress' && message.params.state !== 'inProgress') console.log('DOWNLOAD', message.params.state, message.params.filePath ?? message.params.guid);
    if (message.method === 'Fetch.requestPaused') {
      try {
        const request = message.params.request, parsed = new URL(request.url), method = request.method;
        const body = request.postData ? JSON.parse(request.postData) : null;
        requests.push({ path: parsed.pathname, method, body });
        const result = method === 'OPTIONS' ? null : respond(parsed.pathname, method, body, parsed.searchParams);
        await call('Fetch.fulfillRequest', { requestId: message.params.requestId, responseCode: method === 'OPTIONS' ? 204 : result === undefined ? 404 : 200,
          responseHeaders: [{ name: 'Content-Type', value: 'application/json' }, { name: 'Access-Control-Allow-Origin', value: new URL(url).origin }, { name: 'Access-Control-Allow-Headers', value: 'Authorization, Content-Type' }, { name: 'Access-Control-Allow-Methods', value: 'GET, POST, OPTIONS' }],
          body: Buffer.from(JSON.stringify(result ?? { detail: 'Endpoint absent in test fixture' })).toString('base64') }, message.sessionId);
      } catch (error) { exceptions.push({ fixtureError: String(error) }); }
    }
  };
  const { targetId } = await call('Target.createTarget', { url: 'about:blank' });
  const { sessionId } = await call('Target.attachToTarget', { targetId, flatten: true });
  const c = (method, params) => call(method, params, sessionId);
  const evaluate = async expression => { const r = await c('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }); if (r.exceptionDetails) throw new Error(JSON.stringify(r.exceptionDetails)); return r.result.value; };
  const wait = async expression => { for (let n = 0; n < 150; n++) { if (await evaluate(expression)) return; await new Promise(r => setTimeout(r, 100)); } throw new Error('Timed out: ' + expression); };
  const page = "window.ng.getComponent(document.querySelector('app-statistics-page'))";
  const click = text => evaluate("[...document.querySelectorAll('button')].find(b=>b.offsetParent!==null && b.textContent.trim()===" + JSON.stringify(text) + ")?.click()");
  const capture = async name => { await new Promise(r => setTimeout(r, 500)); const result = await c('Page.captureScreenshot', { format: 'png' }); await writeFile(join(output, name + '.png'), Buffer.from(result.data, 'base64')); console.log('CAPTURE', name); };
  await c('Page.enable'); await c('Runtime.enable');
  await call('Browser.setDownloadBehavior', { behavior: 'allow', downloadPath: output, eventsEnabled: true });
  await c('Fetch.enable', { patterns: [{ urlPattern: 'http://localhost:8080/*' }] });
  await c('Page.addScriptToEvaluateOnNewDocument', { source: "localStorage.setItem('token','statistics-browser-test-only')" });
  await c('Emulation.setDeviceMetricsOverride', { width: 1600, height: 1200, deviceScaleFactor: 1, mobile: false });
  await c('Page.navigate', { url });
  await wait("Boolean(window.ng && document.querySelector('app-statistics-page'))");
  await wait("document.fonts.status === 'loaded'");
  await wait(page + '.workspace.basket() !== null');
  assert.equal(await evaluate("document.querySelectorAll('.app-floating-sidebar').length"), 1);
  assert.equal(await evaluate("document.querySelectorAll('a[href=\"/resultados/estadisticas\"]').length"), 1);
  assert.equal(await evaluate("document.querySelector('a[href=\"/resultados/estadisticas\"]').getAttribute('aria-current')"), 'page');
  await evaluate(`{const w=${page}.workspace;const rows=[];for(let d=1;d<=5;d++)for(const journey of ['DAY','NIGHT'])rows.push({date:w.from().slice(0,8)+String(d).padStart(2,'0'),journey,value:String(100+d*30+(journey==='DAY'?100:0))});w.replaceObservations('FUEL_AGGREGATE',rows);w.consult();}`);
  await wait(page + '.workspace.averages() !== null'); await capture('promedios');
  await click('Escenarios'); await click('COMPARAR'); await wait(page + '.workspace.comparison() !== null'); await capture('escenarios');
  await click('Lubricantes y otros'); await capture('canastas');
  await evaluate("document.querySelector('app-basket-statistics tbody button').click()"); await capture('canastas-detalle'); await evaluate("document.querySelector('dialog .btn-dialog-close').click()");
  await click('Competencia'); await wait("document.querySelectorAll('app-competition-statistics tbody tr').length > 1"); await capture('competencia');
  await evaluate("document.querySelector('app-competition-statistics details').open=true;document.querySelector('.stats-simulator').scrollIntoView({block:'center'})");
  await evaluate("for(const [selector,value] of [['.stats-price-field input','16500'],['[formcontrolname=acquisitionCost]','15000']]){const e=document.querySelector('.stats-simulation-inputs '+selector);e.value=value;e.dispatchEvent(new Event('input',{bubbles:true}));}");
  await click('SIMULAR PRECIO'); await wait("window.ng.getComponent(document.querySelector('app-competition-statistics')).simulation() !== null"); await capture('competencia-simulador');
  const simulation = requests.findLast(r => r.path.endsWith('/simulation')); assert.equal(simulation.body.acquisitionCost, 15000); assert.equal(simulation.body.proposedPrice, 16500);
  await click('Registrar estación');
  await evaluate("for(const [id,value] of [['competitor-name','Estación creada en prueba'],['competitor-brand','Prueba'],['competitor-location','Dirección de prueba']]){const e=document.getElementById(id);e.value=value;e.dispatchEvent(new Event('input',{bubbles:true}));}");
  await capture('competencia-registro-estacion'); await click('REGISTRAR ESTACIÓN');
  await wait("!document.querySelector('dialog[open]')");
  assert.equal(requests.findLast(r => r.path.endsWith('/competitors') && r.method === 'POST').body.name, 'Estación creada en prueba');
  await click('Registrar precio observado');
  await evaluate("for(const [selector,value,event] of [['[formcontrolname=competitorId]','5','change'],['#observed-price','16500.25','input'],['[formcontrolname=observedAt]','2026-01-02T10:30','input']]){const e=document.querySelector('dialog '+selector);e.value=value;e.dispatchEvent(new Event(event,{bubbles:true}));}");
  await capture('competencia-registro-precio'); await click('REGISTRAR OBSERVACIÓN'); await wait("!document.querySelector('dialog[open]')");
  assert.equal(requests.findLast(r => r.path.endsWith('/observations') && r.method === 'POST').body.observedAt, '2026-01-02T15:30:00.000Z');
  await evaluate("document.querySelector('.dashboard-scroll-viewport').scrollTop=0");
  await click('Promedios por turno'); await click('Ver gráficas'); await wait("Boolean(document.querySelector('dialog[open] svg'))"); await capture('visor-graficas');
  await click('Descargar Gráfica (PNG)'); await click('Líneas'); await capture('visor-lineas'); await click('Volver al reporte');
  await click('Exportar balance (.pptx)'); await capture('exportar-balance');
  await evaluate("document.querySelector('app-statistics-balance button').click()");
  for (let n = 0; n < 150; n++) { if ((await readdir(output)).some(f => f.endsWith('.pptx'))) break; await new Promise(r => setTimeout(r, 100)); }
  const files = await readdir(output), pptx = files.find(f => f.endsWith('.pptx')), png = files.find(f => f.startsWith('bellavista-estadisticas') && f.endsWith('.png'));
  if (!pptx || !png) { await capture('export-diagnostics'); console.log('EXPORT DIAGNOSTICS', await evaluate("document.querySelector('dialog').textContent"), exceptions); }
  assert.ok(pptx, 'PPTX download missing'); assert.ok(png, 'PNG download missing');
  const archive = await JSZip.loadAsync(await readFile(join(output, pptx))); assert.equal(Object.keys(archive.files).filter(f => /^ppt\/slides\/slide\d+\.xml$/.test(f)).length, 4);
  const pngBytes = await readFile(join(output, png)); assert.equal(pngBytes.readUInt32BE(16), 1800); assert.equal(pngBytes.readUInt32BE(20), 640);
  await evaluate("document.querySelector('dialog .btn-dialog-close').click()");
  for (const status of ['UNAVAILABLE','NO_DATA']) {
    basketMode = status; await click('Lubricantes y otros'); await click('CONSULTAR'); await wait(page + '.workspace.basket()?.charts.availability === ' + JSON.stringify(status));
    assert.equal(await evaluate("document.querySelectorAll('app-basket-statistics tbody .stats-seller').length"), 0); await capture('canastas-' + status.toLowerCase());
  }
  await c('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
  for (const [tab, name] of [['Promedios por turno','promedios'],['Escenarios','escenarios'],['Lubricantes y otros','canastas'],['Competencia','competencia']]) {
    await click(tab); await evaluate("document.querySelector('.dashboard-scroll-viewport').scrollTop=0"); await capture(name + '-movil');
    assert.equal(await evaluate("document.querySelector('.dashboard-scroll-viewport').scrollWidth <= document.querySelector('.dashboard-scroll-viewport').clientWidth + 1"), true, name + ': mobile page overflow');
    assert.equal(await evaluate("[...document.querySelectorAll('.jr-indicador')].every(e=>e.scrollWidth <= e.clientWidth+1)"), true, name + ': overlapping indicators');
  }
  await evaluate("document.querySelector('.mobile-menu-toggle').click()"); await capture('menu-movil');
  assert.equal(await evaluate("document.querySelector('.mobile-menu-toggle').getAttribute('aria-expanded')"), 'true');
  await evaluate("document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}))");
  await writeFile(join(output, 'results.json'), JSON.stringify({ url, fixturesOnly: true, captures: (await readdir(output)).filter(f => f.endsWith('.png')), requestCount: requests.length, exceptions }, null, 2));
  assert.equal(exceptions.length, 0, JSON.stringify(exceptions));
  console.log('PASS: menu, four tabs, scenario/simulation HTTP, station/observation forms, null/unavailable/no-data, PNG 1800x640, PPTX with four slides, mobile.');
} finally { socket?.close(); chrome.kill(); }
