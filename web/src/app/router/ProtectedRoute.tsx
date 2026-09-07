import type { ReactNode } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { EmptyState } from '@/components/ui/EmptyState';
import { isAuthenticated } from '@/features/auth/store';
import { hasAnyPermission } from '@/lib/permissions';

type ProtectedRouteProps = {
  children?: ReactNode;
  permissions?: string[];
};

export function ProtectedRoute({ children, permissions }: ProtectedRouteProps): JSX.Element {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  if (permissions && permissions.length > 0 && !hasAnyPermission(permissions)) {
    return (
      <EmptyState title="没有权限" description="请联系管理员开通相应权限后重试" />
    );
  }

  return children ? <>{children}</> : <Outlet />;
}
