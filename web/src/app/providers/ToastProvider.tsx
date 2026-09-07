import { Toaster } from 'sonner';
import type { ReactNode } from 'react';

type ToastProviderProps = {
  children: ReactNode;
};

export function ToastProvider({ children }: ToastProviderProps): JSX.Element {
  return (
    <>
      {children}
      <Toaster richColors duration={4200} closeButton position="top-right" />
    </>
  );
}
