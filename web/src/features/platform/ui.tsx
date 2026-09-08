import { type ReactNode } from 'react';
import { useAuthStore } from '@/features/auth/store';
import { PlatformError } from './client';
import { useAction } from './hooks';

export const fieldClass =
  'w-full rounded-xl border border-white/80 bg-white/70 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-primary/30';
export const buttonClass =
  'rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-40';
export const secondaryClass =
  'rounded-xl border border-white/90 bg-white/70 px-4 py-2 text-sm disabled:opacity-40';
export function Panel({ children }: { children: ReactNode }) {
  return (
    <section className="rounded-3xl border border-white/80 bg-white/60 p-5 shadow-sm">
      {children}
    </section>
  );
}
export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="grid gap-1.5 text-sm font-medium">
      {label}
      {children}
    </label>
  );
}
export function Notice({ error }: { error: unknown }) {
  if (!error) return null;
  return (
    <div
      role="alert"
      className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800"
    >
      {error instanceof PlatformError &&
      error.status === 409 &&
      /^\/(portfolio\/records|teaching\/)/.test(error.path ?? '')
        ? '另一端已修改此草稿。请重新打开最新版本后再编辑；当前输入仍保留，请先复制需要保留的内容。'
        : error instanceof Error
          ? error.message
          : String(error)}
    </div>
  );
}
export function Feedback({ action }: { action: ReturnType<typeof useAction> }) {
  return (
    <>
      <Notice error={action.error} />
      {action.success && (
        <p role="status" className="text-sm text-emerald-700">
          {action.success}
        </p>
      )}
    </>
  );
}
export function BranchPicker({
  value,
  onChange,
}: {
  value: string;
  onChange: (v: string) => void;
}) {
  const branches = useAuthStore((s) => s.user?.branches ?? []);
  return (
    <Field label="校区">
      <select className={fieldClass} value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">选择校区</option>
        {branches.map((b) => (
          <option key={b.id} value={String(b.id)}>
            {b.name}
          </option>
        ))}
      </select>
    </Field>
  );
}
