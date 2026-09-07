import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { router } from '@/app/router';
import { AuthProvider } from '@/app/providers/AuthProvider';
import { QueryProvider } from '@/app/providers/QueryProvider';
import { ShortcutProvider } from '@/app/providers/ShortcutProvider';
import { ToastProvider } from '@/app/providers/ToastProvider';
import { ShellProvider } from '@/app/shell/shell-context';
import '@/styles/globals.css';

const rootEl = document.getElementById('root');
if (!rootEl) {
  throw new Error('Root element #root not found');
}

createRoot(rootEl).render(
  <StrictMode>
    <ShellProvider>
      <QueryProvider>
        <AuthProvider>
          <ToastProvider>
            <ShortcutProvider>
              <RouterProvider router={router} />
            </ShortcutProvider>
          </ToastProvider>
        </AuthProvider>
      </QueryProvider>
    </ShellProvider>
  </StrictMode>,
);
