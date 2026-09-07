import { useEffect, useRef } from 'react';
import { matchShortcut } from '@/lib/kbd';

type UseShortcutOptions = {
  enableOnFormTags?: boolean;
  enabled?: boolean;
};

function isFormTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false;
  const tag = target.tagName.toLowerCase();
  if (tag === 'input' || tag === 'textarea' || tag === 'select') return true;
  return target.isContentEditable;
}

export function useShortcut(
  combo: string,
  callback: () => void,
  options: UseShortcutOptions = {},
): void {
  const callbackRef = useRef(callback);
  callbackRef.current = callback;

  useEffect(() => {
    if (options.enabled === false) return undefined;

    const handler = (event: KeyboardEvent) => {
      if ((event.target as HTMLElement | null)?.closest('[data-composing="true"]')) {
        return;
      }
      if (!options.enableOnFormTags && isFormTarget(event.target)) {
        return;
      }
      if (matchShortcut(event, combo)) {
        event.preventDefault();
        callbackRef.current();
      }
    };

    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [combo, options.enableOnFormTags, options.enabled]);
}
