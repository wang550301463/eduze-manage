import type { ReactNode } from 'react';
import { useShell } from '@/app/shell/shell-context';
import { useShortcut } from '@/hooks/useShortcut';

type ShortcutProviderProps = {
  children: ReactNode;
};

function ShortcutBindings(): null {
  const { setPaletteOpen, setHelpOpen } = useShell();

  useShortcut('mod+k', () => setPaletteOpen(true), { enableOnFormTags: true });
  useShortcut('mod+n', () => {
    window.dispatchEvent(new CustomEvent('app:new'));
  });
  useShortcut('?', () => setHelpOpen(true));

  return null;
}

export function ShortcutProvider({ children }: ShortcutProviderProps): JSX.Element {
  return (
    <>
      {children}
      <ShortcutBindings />
    </>
  );
}
