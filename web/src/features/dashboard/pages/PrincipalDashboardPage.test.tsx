import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '@/features/auth/store';
import { PrincipalDashboardPage } from './PrincipalDashboardPage';
vi.mock('../components/StudioWorkbench', () => ({
  StudioWorkbench: ({ teaching }: { teaching: boolean }) => (
    <p>{teaching ? '本人教学' : '校区工作'}</p>
  ),
}));
afterEach(cleanup);
function setUser(roles: string[], permissions: string[]) {
  useAuthStore.setState({
    permissions,
    user: { id: 1, name: '测试', username: 'test', roles, permissions, branches: [] },
  });
}
describe('homepage role scope', () => {
  it.each(['ADVISOR', 'FRONT_DESK'])(
    'does not treat %s without statistics permission as a teacher',
    (role) => {
      setUser([role], ['lesson:read']);
      render(<PrincipalDashboardPage />);
      expect(screen.getByText('校区工作')).toBeInTheDocument();
    },
  );
  it('defaults a teacher to their own lessons even with statistics permission', () => {
    setUser(['TEACHER'], ['lesson:read', 'stat:read']);
    render(<PrincipalDashboardPage />);
    expect(screen.getByText('本人教学')).toBeInTheDocument();
  });
  it('keeps a principal who also teaches on the branch overview', () => {
    setUser(['TEACHER', 'PRINCIPAL'], ['lesson:read', 'stat:read']);
    render(<PrincipalDashboardPage />);
    expect(screen.getByText('校区工作')).toBeInTheDocument();
  });
  it('remains stable while the session is cleared during logout', () => {
    useAuthStore.getState().clearAuth();
    expect(() => render(<PrincipalDashboardPage />)).not.toThrow();
    expect(screen.getByText('校区工作')).toBeInTheDocument();
  });

});
