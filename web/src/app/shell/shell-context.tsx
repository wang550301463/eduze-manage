import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';

type ShellContextValue = {
  paletteOpen: boolean;
  setPaletteOpen: (open: boolean) => void;
  helpOpen: boolean;
  setHelpOpen: (open: boolean) => void;
  mobileNavOpen: boolean;
  setMobileNavOpen: (open: boolean) => void;
};

const ShellContext = createContext<ShellContextValue | null>(null);

export function ShellProvider({ children }: { children: ReactNode }): JSX.Element {
  const [paletteOpen, setPaletteOpen] = useState(false);
  const [helpOpen, setHelpOpen] = useState(false);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  const value = useMemo(
    () => ({
      paletteOpen,
      setPaletteOpen,
      helpOpen,
      setHelpOpen,
      mobileNavOpen,
      setMobileNavOpen,
    }),
    [paletteOpen, helpOpen, mobileNavOpen],
  );

  return <ShellContext.Provider value={value}>{children}</ShellContext.Provider>;
}

export function useShell(): ShellContextValue {
  const ctx = useContext(ShellContext);
  if (!ctx) {
    throw new Error('useShell must be used within ShellProvider');
  }
  return ctx;
}
