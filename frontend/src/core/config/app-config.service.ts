import { Injectable } from '@angular/core';
import { setApiBaseUrl } from './api-base';

interface AppConfig {
  apiProtocol?: string;
  apiHost?: string;
  apiPort?: string;
}

/**
 * Loads the runtime configuration (config.json) generated from environment variables.
 * Empty protocol/host fall back to window.location, so the same build works on any LAN.
 */
@Injectable({ providedIn: 'root' })
export class AppConfigService {
  async load(): Promise<void> {
    let config: AppConfig = {};
    try {
      const response = await fetch('config.json', { cache: 'no-store' });
      if (response.ok) {
        config = (await response.json()) as AppConfig;
      }
    } catch {
      config = {};
    }

    const protocol = config.apiProtocol?.trim() || window.location.protocol;
    const host = config.apiHost?.trim() || window.location.hostname;
    const port = config.apiPort?.trim();
    const authority = port ? `${host}:${port}` : host;

    setApiBaseUrl(`${protocol}//${authority}`);
  }
}
