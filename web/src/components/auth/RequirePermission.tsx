import type { ReactNode } from 'react';
import { hasPermission } from '@/lib/permissions';

type RequirePermissionProps = {
  perm?: string;
  children: ReactNode;
};

/** 无权限时不渲染子节点（用于 Sidebar 项、操作按钮等） */
export function RequirePermission({ perm, children }: RequirePermissionProps): JSX.Element | null {
  if (!perm || hasPermission(perm)) {
    return <>{children}</>;
  }
  return null;
}
