import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MockAdapter from 'axios-mock-adapter';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '@/features/auth/store';
import { apiClient } from '@/lib/axios';
import type { LeaveRecord } from '../../types';
import { LeaveListPage } from '../LeaveListPage';

const leave: LeaveRecord = {
  id: '9223372036854775799',
  branchId: '1',
  studentId: '9223372036854775806',
  studentName: '李小画',
  lessonId: null,
  leaveStartDate: '2026-09-08',
  leaveEndDate: '2026-09-09',
  reason: '感冒休息',
  status: 1,
  statusLabel: '待审批',
  approvedBy: null,
  approvedAt: null,
  createdAt: '2026-09-07T10:00:00',
};

describe('LeaveListPage', () => {
  let mock: MockAdapter;
  let client: QueryClient;

  beforeEach(() => {
    mock = new MockAdapter(apiClient);
    client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    useAuthStore.setState({ permissions: ['leave:read'] });
  });

  afterEach(() => {
    cleanup();
    client.clear();
    mock.restore();
    vi.restoreAllMocks();
    useAuthStore.getState().clearAuth();
  });

  function show() {
    render(
      <QueryClientProvider client={client}>
        <LeaveListPage />
      </QueryClientProvider>,
    );
  }

  it('shows a failed request distinctly from an empty list and retries the current filter', async () => {
    mock.onGet('/leaves').replyOnce(400, { code: 400, message: '请假记录暂时无法读取' });
    mock.onGet('/leaves').reply(200, { code: 0, data: [leave] });
    show();

    expect(await screen.findByRole('alert')).toHaveTextContent('请假记录暂时无法读取');
    expect(screen.queryByText('暂无记录')).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: '重试' }));

    expect(await screen.findByText('感冒休息')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(mock.history.get.map((request) => request.params)).toEqual([
      { status: 1 },
      { status: 1 },
    ]);
  });

  it('opens leave details through a keyboard-accessible record button', async () => {
    const user = userEvent.setup();
    mock.onGet('/leaves').reply(200, { code: 0, data: [leave] });
    show();
    const record = await screen.findByRole('button', { name: /李小画.*2026-09-08/ });
    await user.tab();
    await user.tab();
    await user.tab();
    await user.tab();
    expect(record).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(screen.getByRole('dialog', { name: '请假详情' })).toHaveTextContent('感冒休息');
  });

  it('keeps approval actions outside the detail button and preserves their confirmation behavior', async () => {
    useAuthStore.getState().setUser({
      id: 1,
      name: '老师',
      username: 'teacher',
      roles: [],
      branches: [],
      permissions: ['leave:read', 'leave:approve'],
    });
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    mock.onGet('/leaves').reply(200, { code: 0, data: [leave] });
    show();
    const record = await screen.findByRole('button', { name: /李小画.*2026-09-08/ });
    const approve = screen.getByRole('button', { name: '批准' });
    expect(record).not.toContainElement(approve);
    await userEvent.click(approve);
    expect(confirm).toHaveBeenCalledWith('确认批准该请假？');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(mock.history.post).toHaveLength(0);
  });

  it.each([
    ['批准', 'approve', 2],
    ['拒绝', 'reject', 3],
  ] as const)(
    'closes the detail sheet after %s so a stale pending request cannot be reviewed again',
    async (label, action, status) => {
      useAuthStore.getState().setUser({
        id: 1,
        name: '老师',
        username: 'teacher',
        roles: [],
        branches: [],
        permissions: ['leave:read', 'leave:approve'],
      });
      vi.spyOn(window, 'confirm').mockReturnValue(true);
      mock.onGet('/leaves').replyOnce(200, { code: 0, data: [leave] });
      mock.onGet('/leaves').reply(200, { code: 0, data: [] });
      mock
        .onPost(`/leaves/${leave.id}/${action}`)
        .reply(200, { code: 0, data: { ...leave, status } });
      show();

      await userEvent.click(await screen.findByRole('button', { name: /李小画.*2026-09-08/ }));
      const details = screen.getByRole('dialog', { name: '请假详情' });
      await userEvent.click(within(details).getByRole('button', { name: label }));

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
      expect(await screen.findByText('暂无记录')).toBeInTheDocument();
      expect(mock.history.post).toHaveLength(1);
      expect(mock.history.post[0].url).toBe(`/leaves/${leave.id}/${action}`);
    },
  );
});
