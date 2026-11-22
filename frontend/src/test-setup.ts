// The setup file is bundled apart from the specs (own copy of api-base), so the origin goes through globalThis,
// which api-base reads when it is first evaluated.
(globalThis as { __API_BASE_URL__?: string }).__API_BASE_URL__ = 'http://localhost:8080';
