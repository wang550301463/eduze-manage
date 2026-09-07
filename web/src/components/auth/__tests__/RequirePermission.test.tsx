import { render, screen } from '@testing-library/react';
import { describe, expect, it, beforeEach } from 'vitest';
import { RequirePermission } from '@/components/auth/RequirePermission';
import { useAuthStore } from '@/features/auth/store';

describe('RequirePermission', () => {
  beforeEach(() => {
    useAuthStore.setState({ permissions: [] });
  });

  it('无权限时不渲染子节点', () => {
    render(
      <RequirePermission perm="student:write">
        <button type="button">新建学员</button>
      </RequirePermission>,
    );
    expect(screen.queryByRole('button', { name: '新建学员' })).toBeNull();
  });

  it('有权限时渲染子节点', () => {
    useAuthStore.setState({ permissions: ['student:write'] });
    render(
      <RequirePermission perm="student:write">
        <button type="button">新建学员</button>
      </RequirePermission>,
    );
    expect(screen.getByRole('button', { name: '新建学员' })).toBeInTheDocument();
  });

  it('未指定 perm 时始终渲染', () => {
    render(
      <RequirePermission>
        <span>可见</span>
      </RequirePermission>,
    );
    expect(screen.getByText('可见')).toBeInTheDocument();
  });
});
