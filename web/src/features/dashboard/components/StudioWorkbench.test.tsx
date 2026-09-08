import { render, screen, waitFor, cleanup } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '@/features/auth/store';
import { scheduleApi } from '@/features/teacher/api';
import { fetchDashboardKpis } from '@/features/attendance/api';
import { StudioWorkbench } from './StudioWorkbench';
vi.mock('@/features/teacher/api', () => ({ scheduleApi: { myWeek: vi.fn(), byTeacher: vi.fn() } }));
vi.mock('@/features/attendance/api', () => ({ fetchDashboardKpis: vi.fn() }));
function show(teaching = false) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <StudioWorkbench teaching={teaching} />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
beforeEach(() => {
  vi.clearAllMocks();
  useAuthStore.setState({
    user: {
      id: 1,
      name: '林老师',
      username: 'lin',
      roles: [],
      permissions: ['lesson:read'],
      branches: [{ id: 7, name: '艺术校区', code: 'ART' }],
    },
    permissions: ['lesson:read'],
  });
  vi.mocked(scheduleApi.myWeek).mockResolvedValue({ weekStart: '2026-09-07', columns: [] });
  vi.mocked(scheduleApi.byTeacher).mockResolvedValue({ weekStart: '2026-09-07', columns: [] });
});
afterEach(cleanup);
describe('studio workbench permissions and feedback', () => {
  it('loads only the teacher schedule for teaching view, without requesting business statistics', async () => {
    show(true);
    await screen.findByText('今天没有安排课次');
    expect(scheduleApi.myWeek).toHaveBeenCalled();
    expect(scheduleApi.byTeacher).not.toHaveBeenCalled();
    expect(fetchDashboardKpis).not.toHaveBeenCalled();
    expect(screen.queryByText('请假审批')).not.toBeInTheDocument();
  });
  it('does not fall back to another branch when no branch is assigned', async () => {
    useAuthStore.setState({
      user: { ...useAuthStore.getState().user!, branches: [] },
      permissions: ['lesson:read', 'stat:read'],
    });
    show();
    await screen.findByText('分配校区后，即可查看课堂');
    expect(scheduleApi.byTeacher).not.toHaveBeenCalled();
    expect(fetchDashboardKpis).not.toHaveBeenCalled();
  });
  it('shows a failed schedule as an error, not an empty classroom', async () => {
    vi.mocked(scheduleApi.myWeek).mockRejectedValue(new Error('offline'));
    show(true);
    await waitFor(() => expect(screen.getByText('课表加载失败，请重试。')).toBeInTheDocument());
    expect(screen.queryByText('今天没有安排课次')).not.toBeInTheDocument();
  });
});
