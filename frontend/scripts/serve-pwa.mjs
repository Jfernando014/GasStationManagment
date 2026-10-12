import { createServer } from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import { resolve, sep, extname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../dist/frontend-tmp/browser/', import.meta.url));
const port = Number(process.argv[2] ?? 4300);
const types = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.webmanifest': 'application/manifest+json; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff2': 'font/woff2'
};

await stat(resolve(root, 'ngsw.json'));
if (!Number.isInteger(port) || port < 1 || port > 65535) {
  throw new Error('Provide a valid local port.');
}

// Local production preview only: serves static files and Angular routes, never the API.
createServer(async (request, response) => {
  try {
    if (!['GET', 'HEAD'].includes(request.method)) {
      response.writeHead(405).end();
      return;
    }
    const pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname);
    if (pathname === '/api' || pathname.startsWith('/api/')) {
      response.writeHead(404).end();
      return;
    }
    let filename = resolve(root, `.${pathname}`);
    if (!filename.startsWith(root.endsWith(sep) ? root : root + sep) && filename !== resolve(root)) {
      response.writeHead(403).end();
      return;
    }
    try {
      if (!(await stat(filename)).isFile()) filename = resolve(root, 'index.html');
    } catch {
      if (extname(pathname)) {
        response.writeHead(404).end();
        return;
      }
      filename = resolve(root, 'index.html');
    }
    const body = await readFile(filename);
    response.writeHead(200, {
      'Content-Type': types[extname(filename)] ?? 'application/octet-stream',
      'Cache-Control': 'no-store',
      'Content-Length': body.length
    });
    response.end(request.method === 'HEAD' ? undefined : body);
  } catch {
    response.writeHead(400).end();
  }
}).listen(port, '127.0.0.1', () => {
  console.log(`PWA production preview: http://localhost:${port} (Ctrl+C to stop)`);
});
