let apiBaseUrl = `${window.location.protocol}//${window.location.hostname}`;

/** Overrides the API origin (used by AppConfigService after loading config.json). */
export function setApiBaseUrl(value: string): void {
  apiBaseUrl = value.replace(/\/+$/, '');
}

/** Origin of the backend API (protocol//host:port), resolved at runtime from config.json + window.location. */
export function getApiBaseUrl(): string {
  return apiBaseUrl;
}
