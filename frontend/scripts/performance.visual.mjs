// Test-only fixtures. All backend traffic, including the existing journal-open flow, is intercepted.
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdir, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const origin = process.argv[2] ?? 'http://127.0.0.1:4201';
const output = resolve(process.argv[3] ?? join(tmpdir(), 'scrum108-performance-review'));
await mkdir(output, { recursive: true });
let mode = 'AVAILABLE'; const requests = [], exceptions = [], captures = [], layouts = [];
const date = '2026-08-03';
const journal = { id: 10, date, consecutive: 100, status: 'CLOSED', closedAt: '2026-08-04T07:00:00Z', steps: [] };
const people = [120, 90, 80].map((sales, i) => ({ workerId: `10000000-0000-0000-0000-${String(i + 1).padStart(12, '0')}`,
  recordedName: ['Vendedora de prueba', 'Vendedor nocturno de prueba', 'Vendedor histórico de prueba'][i], sales, target: 100,
  availability: 'AVAILABLE', reason: null, ratio: { numerator: sales, denominator: 100 }, percent: sales, targetSource: 'assigned-test-target' }));
function report(search) {
  const from = search.get('from'), to = search.get('to'), measure = search.get('measure');
  const start = Date.parse(from + 'T00:00:00Z'), end = Date.parse(to + 'T00:00:00Z');
  const coverage = Array.from({ length: (end - start) / 86400000 + 1 }, (_, i) => ({ date: new Date(start + i * 86400000).toISOString().slice(0, 10), status: 'PROCESSED' }));
  const scenarios = people.map((p, i) => ({ workerId: p.workerId, recordedName: p.recordedName,
    scenario: { weekday: 'MONDAY', period: i === 1 ? 'NIGHT' : 'DAY', dayType: 'REGULAR', workedCode: i === 1 ? 'NOCHE' : 'DIA', effectiveRole: 'TITULAR' },
    sales: p.sales, observations: 1, availability: 'AVAILABLE', reason: null }));
  const common = () => ({ window: { from, to }, measure, availability: 'AVAILABLE', reason: null, coverage });
  const compliance = { ...common(), sellers: people.map(p => ({ ...p })) };
  const comparison = { ...common(), baselineDefinition: 'ALL_CONFIRMED_EQUIVALENT_SELLER_SHIFTS_IN_REQUESTED_WINDOW_INCLUDING_SELLER', sellers: scenarios.map((s, i) => ({ ...s,
    sellerAverage: s.sales, stationSales: i === 1 ? 90 : 200, stationObservations: i === 1 ? 1 : 2, stationAverage: i === 1 ? 90 : 100,
    relativeAverage: { numerator: s.sales * (i === 1 ? 1 : 2), denominator: i === 1 ? 90 : 200 }, relativePercent: i === 1 ? 100 : s.sales })) };
  const normalization = { ...common(), sellers: scenarios.map((s, i) => ({ ...s, multiplier: i === 1 ? 1.5 : 1, normalizedSales: s.sales * (i === 1 ? 1.5 : 1), multiplierSource: i === 1 ? 'approved-night-test-source' : 'approved-day-test-source' })) };
  if (mode === 'PARTIAL' || mode === 'MISSING_MULTIPLIER') {
    normalization.availability = mode === 'PARTIAL' ? 'PARTIAL' : 'UNAVAILABLE'; normalization.reason = 'INCOMPLETE_CALCULATIONS';
    normalization.sellers.forEach((s, i) => { if (i === 1 || mode === 'MISSING_MULTIPLIER') Object.assign(s, { availability: 'UNAVAILABLE', reason: 'MULTIPLIER_NOT_PROVIDED', multiplier: null, normalizedSales: null, multiplierSource: null }); });
  }
  if (['NO_DATA', 'PENDING', 'UNAVAILABLE'].includes(mode)) {
    comparison.sellers = []; normalization.sellers = [];
    for (const r of [comparison, normalization]) Object.assign(r, { availability: mode === 'NO_DATA' ? 'NO_DATA' : 'UNAVAILABLE', reason: mode === 'NO_DATA' ? 'NO_CONFIRMED_SELLER_CLOSES' : mode === 'PENDING' ? 'HISTORY_INCOMPLETE' : 'SHIFT_CLASSIFICATION_NOT_PROVIDED' });
    if (mode !== 'UNAVAILABLE') {
      compliance.sellers = []; compliance.availability = comparison.availability; compliance.reason = comparison.reason;
      coverage.forEach(c => c.status = mode === 'PENDING' ? 'PENDING' : 'PROCESSED_EMPTY');
    }
  }
  return { source: 'CONFIRMED_SHIFT_SALES_HISTORY', unit: measure === 'BASKET_COP' ? 'COP' : 'GALLONS', outputScale: 2, roundingMode: 'HALF_EVEN', compliance, comparison, normalization };
}
const chrome = spawn(process.env.CHROME_PATH ?? 'C:/Program Files/Google/Chrome/Application/chrome.exe', ['--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check', '--disable-background-networking', '--disable-extensions', '--remote-debugging-port=0', '--user-data-dir=' + join(output, 'chrome-profile'), 'about:blank'], { windowsHide: true, stdio: ['ignore', 'ignore', 'pipe'] });
let socket;
try {
  const endpoint = await new Promise((resolveEndpoint, reject) => {
    let log = ''; const timer = setTimeout(() => reject(new Error('Chrome timeout')), 20000);
    chrome.stderr.on('data', data => { log += data; const m = log.match(/DevTools listening on (ws:\/\/[^\s]+)/); if (m) { clearTimeout(timer); resolveEndpoint(m[1]); } });
    chrome.on('error', error => { clearTimeout(timer); reject(error); });
  });
  socket = new WebSocket(endpoint); await new Promise((resolveOpen, reject) => { socket.onopen = resolveOpen; socket.onerror = reject; });
  let next = 0; const pending = new Map();
  const call = (method, params = {}, sessionId) => new Promise((resolveCall, reject) => { const id = ++next; pending.set(id, { resolve: resolveCall, reject }); socket.send(JSON.stringify({ id, method, params, sessionId })); });
  socket.onmessage = async event => {
    const m = JSON.parse(event.data), task = pending.get(m.id);
    if (task) { pending.delete(m.id); m.error ? task.reject(new Error(JSON.stringify(m.error))) : task.resolve(m.result); }
    if (m.method === 'Runtime.exceptionThrown') exceptions.push(m.params.exceptionDetails);
    if (m.method !== 'Fetch.requestPaused') return;
    try {
      const request = m.params.request, u = new URL(request.url); requests.push({ method: request.method, path: u.pathname, query: u.search });
      assert.ok(['GET', 'OPTIONS'].includes(request.method) || (request.method === 'POST' && u.pathname.endsWith('/journals/open')), 'Unexpected write');
      let body = [], status = 200;
      if (u.pathname.endsWith('/seller-performance')) { status = mode === 'ERROR' ? 502 : 200; body = mode === 'ERROR' ? { code: 'PERFORMANCE_SOURCE_FAILURE', detail: 'Test source failure' } : report(u.searchParams); }
      else if (u.pathname.endsWith('/journals/open')) body = journal;
      else if (u.pathname.endsWith('/closing-summary')) body = { journalId: 10, journalDate: date, consecutive: 100, status: 'CLOSED', closedAt: journal.closedAt, steps: [], fuelVariations: [], newAutonomyAlerts: [], completedStepsCount: 6, newAlertsCount: 0 };
      else if (u.pathname.includes('/sales/downloads/')) { status = 404; body = { detail: 'Archivo no disponible en esta prueba' }; }
      else if (u.pathname.endsWith('/alerts/count')) body = { pending: 0 };
      await call('Fetch.fulfillRequest', { requestId: m.params.requestId, responseCode: request.method === 'OPTIONS' ? 204 : status,
        responseHeaders: [{ name: 'Content-Type', value: 'application/json' }, { name: 'Access-Control-Allow-Origin', value: origin }, { name: 'Access-Control-Allow-Headers', value: 'Authorization, Content-Type' }, { name: 'Access-Control-Allow-Methods', value: 'GET, POST, OPTIONS' }], body: Buffer.from(JSON.stringify(body)).toString('base64') }, m.sessionId);
    } catch (error) { exceptions.push({ fixtureError: String(error) }); }
  };
  const { targetId } = await call('Target.createTarget', { url: 'about:blank' });
  const { sessionId } = await call('Target.attachToTarget', { targetId, flatten: true });
  const c = (method, params = {}) => call(method, params, sessionId);
  const evaluate = async expression => { const r = await c('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }); if (r.exceptionDetails) throw new Error(JSON.stringify(r.exceptionDetails)); return r.result.value; };
  const wait = async expression => { for (let n = 0; n < 200; n++) { try { if (await evaluate(expression)) return; } catch { /* Navigation context transition. */ } await new Promise(r => setTimeout(r, 100)); } throw new Error('Timed out: ' + expression); };
  const capture = async name => { await new Promise(r => setTimeout(r, 300)); const r = await c('Page.captureScreenshot', { format: 'png' }); await writeFile(join(output, name + '.png'), Buffer.from(r.data, 'base64')); captures.push(name + '.png'); };
  const click = text => evaluate(`[...document.querySelectorAll('button')].find(b=>b.offsetParent!==null && b.textContent.trim()===${JSON.stringify(text)})?.click()`);
  await c('Page.enable'); await c('Emulation.setDeviceMetricsOverride', { width: 1600, height: 1200, deviceScaleFactor: 1, mobile: false });
  await c('Page.navigate', { url: pathToFileURL(resolve('../docs/references/prototipo/css/index.html')).href + '#/resultados/rendimiento' });
  await wait("Boolean(document.querySelector('#perf-chart-bars-wrap'))"); await capture('prototipo-frente-estacion');
  await click('Ajustado por turno'); await capture('prototipo-ajustado');
  await c('Runtime.enable'); await c('Fetch.enable', { patterns: [{ urlPattern: 'http://localhost:8080/*' }] });
  const payload = Buffer.from(JSON.stringify({ sub: 'browser-test-only', exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url');
  await c('Page.addScriptToEvaluateOnNewDocument', { source: `localStorage.setItem('token','test.${payload}.test')` });
  await c('Page.navigate', { url: origin + '/jornada/cerrada?date=' + date });
  await wait("Boolean(document.querySelector('#btn-opcion-rendimiento'))");
  await evaluate("document.querySelector('#btn-opcion-rendimiento').click()");
  const page = "window.ng.getComponent(document.querySelector('app-performance-page'))";
  await wait("Boolean(window.ng && document.querySelector('app-performance-page'))"); await wait(page + '.data() !== null');
  assert.equal(await evaluate(page + '.data().compliance.window.from'), date); assert.equal(await evaluate(page + '.data().compliance.window.to'), date);
  assert.ok(requests.some(r => r.path.endsWith('/seller-performance') && new URLSearchParams(r.query).get('from') === date));
  await capture('estacion-escritorio'); await click('Ajustado por turno'); await capture('ajustado-escritorio');
  await click('Ver gráfica'); await wait("Boolean(document.querySelector('dialog'))"); await capture('visor-grafica');
  await evaluate("document.querySelector('dialog .btn-dialog-close').click()");
  for (const state of ['PARTIAL', 'MISSING_MULTIPLIER', 'UNAVAILABLE', 'NO_DATA', 'PENDING', 'ERROR']) {
    mode = state; await evaluate(page + '.consult()'); await wait(page + '.loading() === false'); await capture('estado-' + state.toLowerCase());
    layouts.push({ state, ...await evaluate(`(() => {
      const factors = document.querySelector('.readonly-config-callout'), chips = document.querySelector('.vendor-chips-selector'), notice = document.querySelector('.performance-page > .performance-notice');
      return { factorsToNotice: factors && notice ? notice.getBoundingClientRect().top - factors.getBoundingClientRect().bottom : null,
        chipsHeight: chips?.getBoundingClientRect().height ?? null, factorsMargin: factors ? getComputedStyle(factors).marginBottom : null,
        chipsMargins: chips ? [getComputedStyle(chips).marginTop, getComputedStyle(chips).marginBottom] : null };
    })()`) });
    if (['PENDING', 'NO_DATA'].includes(state)) {
      assert.equal(layouts.at(-1).chipsHeight, null, 'Empty vendor selector must not take up space');
      assert.equal(layouts.at(-1).factorsToNotice, 14, 'Availability must follow the standard page gap');
    }
    const reason = state === 'PENDING' ? 'El historial del período aún no está completo.'
      : state === 'UNAVAILABLE' ? 'Clasificación histórica de turnos no disponible.'
        : ['PARTIAL', 'MISSING_MULTIPLIER'].includes(state) ? 'Factor de ajuste no disponible.' : null;
    if (reason) assert.ok(await evaluate(`document.querySelector('app-performance-page').textContent.includes(${JSON.stringify(reason)})`));
    if (state === 'MISSING_MULTIPLIER') assert.equal(await evaluate(page + '.chartSeries().length'), 0);
    if (state === 'ERROR') assert.equal(await evaluate(page + '.error().length > 0'), true);
  }
  mode = 'AVAILABLE'; await evaluate(page + '.consult()'); await wait(page + '.data() !== null');
  await click('Balance y descargas'); await wait("Boolean(document.querySelector('app-daily-downloads'))");
  assert.equal(await evaluate("document.querySelector('dialog input[type=date]').value"), date);
  const downloadDate = '2026-07-10';
  await evaluate(`${page}.downloadDate.setValue('${downloadDate}')`);
  await wait(`document.querySelector('dialog input[type=date]').value === '${downloadDate}'`);
  assert.equal(await evaluate("[...document.querySelectorAll('app-daily-downloads button')].filter(b=>b.disabled).length"), 3);
  assert.ok(await evaluate("document.querySelector('dialog').textContent.includes('independiente del período de rendimiento')"));
  assert.equal(await evaluate("document.querySelector('dialog input[type=date]').hasAttribute('min') || document.querySelector('dialog input[type=date]').hasAttribute('max')"), false);
  await capture('descargas-dia');
  await evaluate("document.querySelector('button[aria-label=\"Descargar PLANILLA VENTAS\"]').click()");
  await wait("Boolean(document.querySelector('.daily-downloads-message--error'))"); await capture('descarga-no-disponible');
  assert.ok(requests.some(r => r.method === 'GET' && r.path === `/api/v1/sales/downloads/${downloadDate}/sales-sheet`));
  await evaluate("document.querySelector('button[aria-label=\"Descargar LUBRICANTES Y VARIOS\"]').click()");
  await wait("Boolean(document.querySelector('.daily-downloads-message--error'))");
  assert.ok(requests.some(r => r.method === 'GET' && r.path === `/api/v1/sales/downloads/${downloadDate}/lubricants-and-misc`));
  assert.ok(!requests.some(r => /sales\/downloads/.test(r.path) && !/sales-sheet|lubricants-and-misc/.test(r.path)));
  await evaluate(`${page}.downloadDate.setValue('2026-07-11')`);
  await wait("document.querySelector('dialog input[type=date]').value === '2026-07-11'");
  assert.equal(await evaluate("document.querySelector('.daily-downloads-message--error') === null"), true, 'Do not keep errors of another download date');
  await evaluate(`${page}.downloadDate.setValue('${downloadDate}')`);
  await evaluate("document.querySelector('dialog .btn-dialog-close').click()");
  await c('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
  for (const text of ['Frente a la estación', 'Ajustado por turno']) {
    await click(text); await evaluate("document.querySelector('.dashboard-scroll-viewport').scrollTop=0"); await capture(text.startsWith('Frente') ? 'estacion-movil' : 'ajustado-movil');
    assert.equal(await evaluate("document.querySelector('.dashboard-scroll-viewport').scrollWidth <= document.querySelector('.dashboard-scroll-viewport').clientWidth + 1"), true, 'Mobile page overflow');
    assert.equal(await evaluate("{const a=document.querySelector('.header-trail').getBoundingClientRect(),b=document.querySelector('.header-action-tray').getBoundingClientRect();a.bottom<=b.top+1 || a.right<=b.left+1}"), true, 'Header actions overlap the breadcrumb');
    await evaluate("document.querySelector('section[role=tabpanel] .rs-tabla-wrap').scrollIntoView({block:'start'})");
    await capture(text.startsWith('Frente') ? 'estacion-tabla-movil' : 'ajustado-tabla-movil');
    await evaluate("document.querySelector('.performance-chart').scrollIntoView({block:'start'})");
    await capture(text.startsWith('Frente') ? 'estacion-grafica-movil' : 'ajustado-grafica-movil');
  }
  await evaluate("document.querySelector('.dashboard-scroll-viewport').scrollTop=0");
  await click('Balance y descargas'); await wait("Boolean(document.querySelector('app-daily-downloads'))"); await capture('descargas-movil');
  assert.equal(await evaluate("document.querySelector('dialog input[type=date]').value"), downloadDate);
  await evaluate("document.querySelector('dialog .btn-dialog-close').click()");
  mode = 'PENDING'; await evaluate(page + '.consult()'); await wait(page + '.loading() === false');
  await evaluate("document.querySelector('.readonly-config-callout').scrollIntoView({block:'start'})"); await capture('historial-incompleto-movil');
  assert.equal(await evaluate("{const a=document.querySelector('.readonly-config-callout').getBoundingClientRect(),b=document.querySelector('.performance-page > .performance-notice').getBoundingClientRect();b.top-a.bottom}"), 14);
  await evaluate("document.querySelector('.mobile-menu-toggle').click()"); await capture('menu-movil');
  await evaluate("document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}))");
  await evaluate("document.querySelector('a[href=\"/jornada/cerrada?date=2026-08-03\"]').click()");
  await wait("Boolean(document.querySelector('#btn-opcion-rendimiento'))");
  assert.equal(await evaluate("new URL(location.href).searchParams.get('date')"), date);
  assert.equal(exceptions.length, 0, JSON.stringify(exceptions));
  await writeFile(join(output, 'results.json'), JSON.stringify({ origin, fixturesOnly: true, captures, requests, exceptions, layouts }, null, 2));
  console.log(`PASS: ${captures.length} captures; prototype, C-09 desktop/mobile, states, chart, daily downloads, operational-date navigation and return. No real backend traffic.`); console.log(output);
} finally { socket?.close(); chrome.kill(); }
