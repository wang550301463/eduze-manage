const IS_MAC =
  typeof navigator !== 'undefined' &&
  /Mac|iPhone|iPad|iPod/.test(navigator.platform ?? navigator.userAgent);

export function isMacPlatform(): boolean {
  return IS_MAC;
}

export function normalizeShortcut(combo: string): string[] {
  return combo
    .toLowerCase()
    .split('+')
    .map((p) => p.trim())
    .filter(Boolean);
}

export function matchShortcut(event: KeyboardEvent, combo: string): boolean {
  const parts = normalizeShortcut(combo);
  const key = parts[parts.length - 1];
  const needsMod = parts.includes('mod');
  const needsShift = parts.includes('shift');
  const needsAlt = parts.includes('alt');

  const modPressed = IS_MAC ? event.metaKey : event.ctrlKey;

  if (needsMod && !modPressed) return false;
  if (!needsMod && modPressed && key !== 'mod') return false;
  if (needsShift !== event.shiftKey) return false;
  if (needsAlt !== event.altKey) return false;

  const eventKey = event.key.length === 1 ? event.key.toLowerCase() : event.key.toLowerCase();
  if (key === 'mod') return false;
  if (key === 'escape') return eventKey === 'escape';
  if (key === '?') return event.key === '?' || (event.shiftKey && event.key === '/');
  return eventKey === key;
}

export function formatShortcutLabel(combo: string): string {
  const parts = normalizeShortcut(combo);
  return parts
    .map((p) => {
      if (p === 'mod') return IS_MAC ? '⌘' : 'Ctrl';
      if (p === 'shift') return '⇧';
      if (p === 'alt') return IS_MAC ? '⌥' : 'Alt';
      if (p === '?') return '?';
      return p.toUpperCase();
    })
    .join(IS_MAC && parts.length === 1 && parts[0] !== '?' ? '' : '+');
}
