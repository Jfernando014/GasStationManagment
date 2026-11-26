import { DestroyRef, Signal, inject, signal } from '@angular/core';

const PHONE_QUERY = '(max-width: 640px)';

/** Whether the screen is a phone, kept up to date. Call it where `inject` is allowed. */
export function injectIsPhone(): Signal<boolean> {
  const destroyRef = inject(DestroyRef);
  const media = typeof window === 'undefined' ? null : window.matchMedia?.(PHONE_QUERY) ?? null;
  const phone = signal(media?.matches ?? false);
  if (!media) return phone;
  // Both events: some embedded browsers do not emit the `change` of the media query.
  const sync = () => phone.set(media.matches);
  media.addEventListener('change', sync);
  window.addEventListener('resize', sync);
  destroyRef.onDestroy(() => {
    media.removeEventListener('change', sync);
    window.removeEventListener('resize', sync);
  });
  return phone;
}
