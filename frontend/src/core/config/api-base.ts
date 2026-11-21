/**
 * The unit-test builder bundles the setup file apart from the specs, so the two get separate copies of this module and
 * a setApiBaseUrl() call in the setup never reaches the specs. The setup leaves the origin on globalThis instead.
 */
const testOrigin = (globalThis as { __API_BASE_URL__?: string }).__API_BASE_URL__;

let apiBaseUrl = testOrigin ?? `${window.location.protocol}//${window.location.hostname}`;

/** Overrides the API origin (used by AppConfigService after loading config.json). */
export function setApiBaseUrl(value: string): void {
  apiBaseUrl = value.replace(/\/+$/, '');
}

/** Origin of the backend API (protocol//host:port), resolved at runtime from config.json + window.location. */
export function getApiBaseUrl(): string {
  return apiBaseUrl;
}
