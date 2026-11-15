// Browser-only fixtures. Every backend request is intercepted; no real data is read or written.
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdir, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const url = process.argv[2] ?? 'http://127.0.0.1:4201/resultados/metas';
const output = resolve(process.argv[3] ?? join(tmpdir(), 'scrum108-compliance-review'));
await mkdir(output, { recursive: true });
let mode = 'AVAILABLE';
const requests = [], exceptions = [], captures = [];
function report(search) {
  const from = search.get('from'), to = search.get('to');
  const first = Date.parse(from + 'T00:00:00Z'), last = Date.parse(to + 'T00:00:00Z');
  const coverage = Array.from({ length: (last - first) / 86400000 + 1 }, (_, i) => ({ date: new Date(first + i * 86400000).toISOString().slice(0, 10), status: 'PROCESSED' }));
  const sellers = [{ workerId: '10000000-0000-0000-0000-000000000001', recordedName: 'Vendedora de prueba', sales: 120, target: 100,
    availability: 'AVAILABLE', reason: null, ratio: { numerator: 120, denominator: 100 }, percent: 120, targetSource: 'browser-test-only' }];
  let availability = mode, reason = null;
  if (mode === 'PARTIAL' || mode === 'MISSING_TARGET') {
    if (mode === 'MISSING_TARGET') sellers.length = 0;
    sellers.push({ workerId: '20000000-0000-0000-0000-000000000002', recordedName: 'Vendedor histórico de prueba', sales: 0, target: null,
      availability: 'UNAVAILABLE', reason: 'TARGET_NOT_PROVIDED', ratio: null, percent: null, targetSource: null });
    availability = mode === 'PARTIAL' ? 'PARTIAL' : 'UNAVAILABLE'; reason = 'INCOMPLETE_CALCULATIONS';
  }
  if (mode === 'NO_DATA' || mode === 'PENDING') {
    sellers.length = 0; coverage.forEach(c => c.status = mode === 'NO_DATA' ? 'PROCESSED_EMPTY' : 'PENDING');
    availability = mode === 'NO_DATA' ? 'NO_DATA' : 'UNAVAILABLE';
    reason = mode === 'NO_DATA' ? 'NO_CONFIRMED_SELLER_CLOSES' : 'HISTORY_INCOMPLETE';
  }
  return { source: 'CONFIRMED_SHIFT_SALES_HISTORY', unit: 'GALLONS', outputScale: 2, roundingMode: 'HALF_EVEN',
    compliance: { window: { from, to }, measure: 'FUEL_GALLONS', availability, reason, coverage, sellers },
    comparison: { availability: 'UNAVAILABLE', reason: 'SHIFT_CLASSIFICATION_NOT_PROVIDED' },
    normalization: { availability: 'UNAVAILABLE', reason: 'SHIFT_CLASSIFICATION_NOT_PROVIDED' } };
}
const chrome = spawn(process.env.CHROME_PATH ?? 'C:/Program Files/Google/Chrome/Application/chrome.exe', [
  '--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check', '--disable-background-networking',
  '--disable-extensions', '--remote-debugging-port=0', '--user-data-dir=' + join(output, 'chrome-profile'), 'about:blank'
], { windowsHide: true, stdio: ['ignore', 'ignore', 'pipe'] });
let socket;
try {
  const endpoint = await new Promise((resolveEndpoint, reject) => {
    let log = ''; const timer = setTimeout(() => reject(new Error('Chrome timeout')), 20000);
    chrome.stderr.on('data', data => {
      log += data; const match = log.match(/DevTools listening on (ws:\/\/[^\s]+)/);
      if (match) { clearTimeout(timer); resolveEndpoint(match[1]); }
    });
    chrome.on('error', error => { clearTimeout(timer); reject(error); });
  });
  socket = new WebSocket(endpoint);
  await new Promise((resolveOpen, reject) => { socket.onopen = resolveOpen; socket.onerror = reject; });
  let next = 0; const pending = new Map();
  const call = (method, params = {}, sessionId) => new Promise((resolveCall, reject) => {
    const id = ++next; pending.set(id, { resolve: resolveCall, reject }); socket.send(JSON.stringify({ id, method, params, sessionId }));
  });
  socket.onmessage = async event => {
    const message = JSON.parse(event.data), task = pending.get(message.id);
    if (task) { pending.delete(message.id); message.error ? task.reject(new Error(JSON.stringify(message.error))) : task.resolve(message.result); }
    if (message.method === 'Runtime.exceptionThrown') exceptions.push(message.params.exceptionDetails);
    if (message.method !== 'Fetch.requestPaused') return;
    try {
      const request = message.params.request, parsed = new URL(request.url);
      requests.push({ method: request.method, path: parsed.pathname, query: parsed.search });
      assert.ok(['GET', 'OPTIONS'].includes(request.method), 'Unexpected write from compliance');
      const performance = parsed.pathname.endsWith('/seller-performance');
      const status = request.method === 'OPTIONS' ? 204 : performance && mode === 'ERROR' ? 502 : 200;
      const body = performance ? mode === 'ERROR' ? { code: 'PERFORMANCE_SOURCE_FAILURE', detail: 'Test source failure' } : report(parsed.searchParams) : [];
      await call('Fetch.fulfillRequest', { requestId: message.params.requestId, responseCode: status,
        responseHeaders: [{ name: 'Content-Type', value: 'application/json' }, { name: 'Access-Control-Allow-Origin', value: new URL(url).origin },
          { name: 'Access-Control-Allow-Headers', value: 'Authorization, Content-Type' }, { name: 'Access-Control-Allow-Methods', value: 'GET, OPTIONS' }],
        body: Buffer.from(JSON.stringify(body)).toString('base64') }, message.sessionId);
    } catch (error) { exceptions.push({ fixtureError: String(error) }); }
  };
  const { targetId } = await call('Target.createTarget', { url: 'about:blank' });
  const { sessionId } = await call('Target.attachToTarget', { targetId, flatten: true });
  const c = (method, params = {}) => call(method, params, sessionId);
  const evaluate = async expression => {
    const result = await c('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
    if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails));
    return result.result.value;
  };
  const wait = async expression => {
    for (let n = 0; n < 150; n++) {
      try { if (await evaluate(expression)) return; } catch { /* A navigation/HMR can replace the execution context. */ }
      await new Promise(r => setTimeout(r, 100));
    }
    throw new Error('Timed out: ' + expression);
  };
  const capture = async name => {
    await new Promise(r => setTimeout(r, 300));
    const data = await c('Page.captureScreenshot', { format: 'png' });
    await writeFile(join(output, name + '.png'), Buffer.from(data.data, 'base64')); captures.push(name + '.png');
  };
  await c('Page.enable');
  await c('Emulation.setDeviceMetricsOverride', { width: 1600, height: 1200, deviceScaleFactor: 1, mobile: false });
  // Capture the local, unmodified original before visiting Angular. Its demo data is reference only.
  await c('Page.navigate', { url: pathToFileURL(resolve('../docs/references/prototipo/css/index.html')).href + '#/resultados/metas' });
  await wait("Boolean(document.querySelector('#compliance-table'))");
  await capture('prototipo-cumplimiento');
  await c('Runtime.enable');
  await c('Fetch.enable', { patterns: [{ urlPattern: 'http://localhost:8080/*' }] });
  const payload = Buffer.from(JSON.stringify({ sub: 'browser-test-only', exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url');
  await c('Page.addScriptToEvaluateOnNewDocument', { source: `localStorage.setItem('token','test.${payload}.test')` });
  await c('Page.navigate', { url });
  const page = "window.ng.getComponent(document.querySelector('app-annual-goals-page'))";
  await wait("Boolean(window.ng && document.querySelector('app-annual-goals-page'))");
  await wait(page + '.compliance()?.compliance.availability === "AVAILABLE"');
  await capture('cumplimiento-disponible');
  assert.equal(await evaluate("document.querySelectorAll('#panel-cumplimiento .compliance-seller').length"), 1);
  for (const state of ['PARTIAL', 'MISSING_TARGET', 'NO_DATA', 'PENDING', 'ERROR']) {
    mode = state;
    await evaluate("document.querySelector('#panel-cumplimiento button[type=submit]').click()");
    await wait(page + '.complianceLoading() === false');
    assert.equal(await evaluate(page + '.complianceError().length > 0'), state === 'ERROR');
    if (state === 'MISSING_TARGET') assert.equal(await evaluate("document.querySelectorAll('#panel-cumplimiento app-percent-bar').length"), 0);
    await capture('cumplimiento-' + state.toLowerCase());
  }
  mode = 'PARTIAL'; await evaluate(page + '.consultCompliance()'); await wait(page + '.compliance() !== null');
  await evaluate("{const e=document.querySelector('#goals-seller-search');e.value='histórico';e.dispatchEvent(new Event('input',{bubbles:true}));}");
  await wait("document.querySelectorAll('#panel-cumplimiento .compliance-seller').length === 1");
  assert.equal(await evaluate(page + '.complianceSales()'), '120');
  await capture('cumplimiento-busqueda');
  await evaluate("document.getElementById('tab-metas').click()");
  assert.equal(await evaluate("document.querySelector('details.jr-plegable').open"), false);
  await capture('metas-conservadas');
  assert.equal(exceptions.length, 0, JSON.stringify(exceptions));
  await writeFile(join(output, 'results.json'), JSON.stringify({ url, fixturesOnly: true, captures, requests, exceptions }, null, 2));
  console.log('PASS: C-14 original and Angular; available, partial, missing target, empty, pending, error, search and Metas; no backend writes.');
  console.log(output);
} finally { socket?.close(); chrome.kill(); }
