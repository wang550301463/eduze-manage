import { Suspense, type ReactNode } from 'react';

export function LazyPage({ children }: { children: ReactNode }) {
  return (
    <Suspense fallback={<div className="p-6 text-sm text-muted-foreground">加载中…</div>}>
      {children}
    </Suspense>
  );
}
