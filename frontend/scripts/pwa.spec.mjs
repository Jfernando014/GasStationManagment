import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { join, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createHash } from 'node:crypto';
import { test } from 'node:test';

const frontend = fileURLToPath(new URL('../', import.meta.url));
const build = join(frontend, 'dist/frontend-tmp/browser');
const json = (path) => JSON.parse(readFileSync(path, 'utf8'));
const manifest = json(join(build, 'manifest.webmanifest'));
const worker = json(join(build, 'ngsw.json'));

test('production includes the Angular worker and a linked standalone manifest', () => {
  for (const filename of ['ngsw-worker.js', 'safety-worker.js', 'ngsw.json']) {
    assert.ok(existsSync(join(build, filename)), `${filename} missing; build production first`);
  }
  assert.match(readFileSync(join(build, 'index.html'), 'utf8'), /rel="manifest" href="manifest.webmanifest"/);
  assert.equal(manifest.display, 'standalone');
  assert.match(manifest.name, /EDS Bellavista/);
  assert.equal(manifest.short_name, 'Bellavista');
  assert.equal(manifest.theme_color, '#FF6B00');
  assert.equal(manifest.background_color, '#F4F7FA');
});

test('manifest identity, launch URL and scope remain within its deployment directory', () => {
  for (const base of ['https://example.test/', 'https://example.test/eds/']) {
    const manifestUrl = new URL('manifest.webmanifest', base);
    const scope = new URL(manifest.scope, manifestUrl);
    const start = new URL(manifest.start_url, manifestUrl);
    assert.equal(scope.href, base);
    assert.equal(start.origin, scope.origin);
    assert.ok(start.pathname.startsWith(scope.pathname));
    assert.equal(new URL(manifest.id, manifestUrl).href, base);
  }
});

test('normal and maskable installation icons are real PNGs with declared dimensions', () => {
  const pngSignature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  for (const purpose of ['any', 'maskable']) {
    assert.deepEqual(manifest.icons.filter((icon) => icon.purpose === purpose).map((icon) => icon.sizes),
      ['192x192', '512x512']);
  }
  for (const icon of [...manifest.icons, { src: 'icons/apple-touch-icon.png', sizes: '180x180', type: 'image/png' }]) {
    const path = resolve(build, icon.src);
    assert.ok(path.startsWith(resolve(build) + sep));
    const png = readFileSync(path);
    const [width, height] = icon.sizes.split('x').map(Number);
    assert.equal(icon.type, 'image/png');
    assert.deepEqual(png.subarray(0, 8), pngSignature);
    assert.equal(png.toString('ascii', 12, 16), 'IHDR');
    assert.equal(png.readUInt32BE(16), width);
    assert.equal(png.readUInt32BE(20), height);
    assert.ok(png.includes(Buffer.from('IDAT')) && png.length > 1000, `${icon.src} lacks image data`);
  }
});

test('worker caches only build files and icons, with no API or external URL caching', () => {
  assert.deepEqual(worker.dataGroups, []);
  assert.ok(worker.assetGroups.length > 0);
  for (const group of worker.assetGroups) {
    assert.deepEqual(group.patterns, []);
    for (const url of group.urls) {
      assert.match(url, /^\/(?:index\.html|manifest\.webmanifest|[^/]+\.(?:js|css)|icons\/[^/]+\.(?:png|svg))$/);
      assert.ok(!url.includes('/api/'));
      const file = readFileSync(join(build, url.slice(1)));
      assert.equal(worker.hashTable[url], createHash('sha1').update(file).digest('hex'));
    }
  }
  for (const path of ['/api', '/api/v1/auth/login', '/api/v1/sales/targets']) {
    assert.ok(worker.navigationUrls.some((rule) => !rule.positive && new RegExp(rule.regex).test(path)));
  }
});

test('development build remains without a service worker, and package versions match', () => {
  const workspace = json(join(frontend, 'angular.json')).projects['frontend-tmp'].architect;
  assert.equal(workspace.build.configurations.production.serviceWorker, 'ngsw-config.json');
  assert.equal(workspace.build.options.serviceWorker ?? false, false);
  assert.equal(workspace.build.configurations.development.serviceWorker ?? false, false);
  assert.equal(workspace.serve.defaultConfiguration, 'development');
  const packages = json(join(frontend, 'package-lock.json')).packages;
  assert.equal(packages['node_modules/@angular/service-worker'].version, packages['node_modules/@angular/core'].version);
});
