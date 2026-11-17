import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));

// Carga el .env de la raiz del repo si existe (para desarrollo local fuera de Docker).
const rootEnv = resolve(here, '../../.env');
if (existsSync(rootEnv)) {
  for (const line of readFileSync(rootEnv, 'utf8').split('\n')) {
    const match = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$/);
    if (match && process.env[match[1]] === undefined) {
      process.env[match[1]] = match[2];
    }
  }
}

const config = {
  apiProtocol: process.env.API_PROTOCOL ?? '',
  apiHost: process.env.API_HOST ?? '',
  apiPort: process.env.API_PORT ?? process.env.SERVER_PORT ?? ''
};

writeFileSync(resolve(here, '../public/config.json'), `${JSON.stringify(config, null, 2)}\n`);
