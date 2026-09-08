import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MockAdapter from 'axios-mock-adapter';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { LeaveRecord } from '@/features/attendance/types';
import { useAuthStore } from '@/features/auth/store';
import { apiClient } from '@/lib/axios';
import { StudentLeaveTab } from '../StudentLeaveTab';

const studentId = '9223372036854775806';
const otherStudentId = '9223372036854775805';
const leave: LeaveRecord = {
  id: '9223372036854775799',
  branchId: '1',
  studentId,
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

describe('StudentLeaveTab', () => {
  let mock: MockAdapter;
  let client: QueryClient;

  beforeEach(() => {
    mock = new MockAdapter(apiClient);
    client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    useAuthStore.setState({ permissions: ['leave:read'] });
  });

  afterEach(() => {
    cleanup();
    mock.restore();
    client.clear();
    useAuthStore.getState().clearAuth();
  });

  function show(id: string = studentId) {
    const view = (currentId: string) => (
      <QueryClientProvider client={client}>
        <StudentLeaveTab studentId={currentId} studentName="李小画" enrollNo="ART-2026-018" />
      </QueryClientProvider>
    );
    return { ...render(view(id)), view };
  }

  it('queries the exact student ID and excludes records belonging to another student', async () => {
    mock
      .onGet('/leaves')
      .reply(200, {
        code: 0,
        data: [leave, { ...leave, id: '2', studentId: otherStudentId, reason: '其他学员隐私' }],
      });
    show();

    expect(await screen.findByText('感冒休息')).toBeInTheDocument();
    expect(screen.getByText('待审批')).toBeInTheDocument();
    expect(screen.getByText(/2026-09-08/)).toBeInTheDocument();
    expect(screen.queryByText('其他学员隐私')).not.toBeInTheDocument();
    expect(mock.history.get[0].params).toEqual({ studentId });
    expect(screen.queryByRole('button', { name: '录入请假' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '批准' })).not.toBeInTheDocument();
  });

  it('does not reuse the previous student records when the student changes', async () => {
    mock
      .onGet('/leaves')
      .reply((config) => [
        200,
        { code: 0, data: config.params.studentId === studentId ? [leave] : [] },
      ]);
    const { rerender, view } = show();
    expect(await screen.findByText('感冒休息')).toBeInTheDocument();
    rerender(view(otherStudentId));

    expect(screen.queryByText('感冒休息')).not.toBeInTheDocument();
    expect(await screen.findByText('暂无请假记录')).toBeInTheDocument();
    expect(mock.history.get.map((request) => request.params.studentId)).toEqual([
      studentId,
      otherStudentId,
    ]);
  });

  it('shows loading before an empty result', async () => {
    let finish!: (result: [number, object]) => void;
    mock.onGet('/leaves').reply(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
    show();
    expect(screen.getByRole('status')).toHaveTextContent('正在加载请假记录');
    expect(screen.queryByText('暂无请假记录')).not.toBeInTheDocument();
    await waitFor(() => expect(mock.history.get).toHaveLength(1));
    finish([200, { code: 0, data: [] }]);
    expect(await screen.findByText('暂无请假记录')).toBeInTheDocument();
  });

  it('shows a failed request distinctly from empty and can retry', async () => {
    mock.onGet('/leaves').replyOnce(400, { code: 400, message: '请假查询暂时不可用' });
    mock.onGet('/leaves').reply(200, { code: 0, data: [leave] });
    show();
    expect(await screen.findByRole('alert')).toHaveTextContent('请假查询暂时不可用');
    expect(screen.queryByText('暂无请假记录')).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: '重试' }));
    expect(await screen.findByText('感冒休息')).toBeInTheDocument();
  });

  it('never requests an unscoped list for a missing ID or without read permission', async () => {
    mock.onGet('/leaves').reply(200, { code: 0, data: [] });
    const { rerender, view } = show('');
    await waitFor(() => expect(screen.getByText('请选择学员')).toBeInTheDocument());
    expect(mock.history.get).toHaveLength(0);
    act(() => useAuthStore.setState({ permissions: [] }));
    rerender(view(studentId));
    expect(screen.getByText('暂无查看请假记录的权限')).toBeInTheDocument();
    expect(mock.history.get).toHaveLength(0);
  });

  it('opens a student-locked form only with leave write permission', async () => {
    useAuthStore.setState({ permissions: ['leave:read', 'leave:write'] });
    mock.onGet('/leaves').reply(200, { code: 0, data: [] });
    show();
    await userEvent.click(screen.getByRole('button', { name: '录入请假' }));
    expect(screen.getByRole('dialog')).toHaveTextContent('李小画');
    expect(screen.getByRole('dialog')).toHaveTextContent('ART-2026-018');
    expect(screen.queryByLabelText('搜索学员')).not.toBeInTheDocument();
  });
});
