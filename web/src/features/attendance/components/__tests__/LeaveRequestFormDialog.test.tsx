import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MockAdapter from 'axios-mock-adapter';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { studentApi } from '@/features/student/api';
import { useAuthStore } from '@/features/auth/store';
import type { Student } from '@/features/student/types';
import { apiClient } from '@/lib/axios';
import { LeaveRequestFormDialog } from '../LeaveRequestFormDialog';

const student: Student = {
  id: '9223372036854775806',
  branchId: '1',
  name: '李小画',
  enrollNo: 'ART-2026-018',
  gender: 1,
  status: 1,
  classGroups: [],
  totalRemaining: 10,
  alertLow: false,
};

describe('LeaveRequestFormDialog', () => {
  let mock: MockAdapter;
  let client: QueryClient;

  beforeEach(() => {
    mock = new MockAdapter(apiClient);
    client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    vi.spyOn(studentApi, 'list').mockResolvedValue({
      records: [student],
      total: 1,
      page: 1,
      size: 20,
    });
  });

  afterEach(() => {
    cleanup();
    mock.restore();
    client.clear();
    vi.restoreAllMocks();
    useAuthStore.getState().clearAuth();
  });

  function show(preselectedStudent?: { id: string | number; name: string; enrollNo: string }) {
    const onClose = vi.fn();
    const onDone = vi.fn();
    const view = (open: boolean) => (
      <QueryClientProvider client={client}>
        <LeaveRequestFormDialog
          open={open}
          onClose={onClose}
          onDone={onDone}
          student={preselectedStudent}
        />
      </QueryClientProvider>
    );
    return { ...render(view(true)), view, onClose, onDone };
  }

  function dates(start: string, end: string) {
    fireEvent.change(screen.getByLabelText('开始日期'), { target: { value: start } });
    fireEvent.change(screen.getByLabelText('结束日期'), { target: { value: end } });
  }

  it('searches by name or enrollment number and submits the selected large ID unchanged', async () => {
    const user = userEvent.setup();
    mock.onPost('/leaves').reply(200, { code: 0, data: {} });
    const { onDone, onClose } = show();

    await user.type(screen.getByLabelText('搜索学员'), 'ART-2026-018');
    await waitFor(() =>
      expect(studentApi.list).toHaveBeenLastCalledWith({
        keyword: 'ART-2026-018',
        page: '1',
        size: '20',
      }),
    );
    await user.click(await screen.findByRole('button', { name: /李小画.*ART-2026-018/ }));
    dates('2026-09-08', '2026-09-08');
    await user.click(screen.getByRole('button', { name: '提交' }));

    await waitFor(() => expect(onDone).toHaveBeenCalledOnce());
    expect(JSON.parse(mock.history.post[0].data)).toEqual({
      studentId: student.id,
      leaveStartDate: '2026-09-08',
      leaveEndDate: '2026-09-08',
    });
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('rejects an end date before the start without sending a request', async () => {
    show(student);
    dates('2026-09-10', '2026-09-09');
    await userEvent.click(screen.getByRole('button', { name: '提交' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('结束日期不能早于开始日期');
    expect(mock.history.post).toHaveLength(0);
  });

  it('requires choosing a search result before submission', async () => {
    show();
    await userEvent.type(screen.getByLabelText('搜索学员'), '李小画');
    dates('2026-09-08', '2026-09-08');
    await userEvent.click(screen.getByRole('button', { name: '提交' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('请选择学员');
    expect(mock.history.post).toHaveLength(0);
  });

  it('keeps a preselected student locked and resets dates, reason, and errors when reopened', async () => {
    const { rerender, view } = show(student);
    expect(screen.getByText(/李小画/)).toBeInTheDocument();
    expect(screen.queryByLabelText('搜索学员')).not.toBeInTheDocument();
    expect(studentApi.list).not.toHaveBeenCalled();
    dates('2026-09-10', '2026-09-09');
    await userEvent.type(screen.getByLabelText('原因'), '临时原因');
    await userEvent.click(screen.getByRole('button', { name: '提交' }));
    expect(await screen.findByRole('alert')).toBeInTheDocument();

    rerender(view(false));
    rerender(view(true));
    expect(screen.getByLabelText('开始日期')).toHaveValue('');
    expect(screen.getByLabelText('结束日期')).toHaveValue('');
    expect(screen.getByLabelText('原因')).toHaveValue('');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.getByText(/李小画/)).toBeInTheDocument();
  });

  it('prevents duplicate submissions and preserves the form after a server error for retry', async () => {
    let finish!: (result: [number, object]) => void;
    mock.onPost('/leaves').replyOnce(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
    mock.onPost('/leaves').reply(200, { code: 0, data: {} });
    const { onClose, onDone } = show({ ...student, id: 42 });
    dates('2026-09-08', '2026-09-09');
    await userEvent.click(screen.getByRole('button', { name: '提交' }));

    expect(screen.getByRole('button', { name: '提交中…' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '取消' })).toBeDisabled();
    expect(screen.getByLabelText('开始日期')).toBeDisabled();
    finish([400, { code: 400, message: '请假日期与已有记录重叠' }]);
    expect(await screen.findByRole('alert')).toHaveTextContent('请假日期与已有记录重叠');
    expect(onClose).not.toHaveBeenCalled();
    expect(screen.getByLabelText('开始日期')).toHaveValue('2026-09-08');

    await userEvent.click(screen.getByRole('button', { name: '提交' }));
    await waitFor(() => expect(onDone).toHaveBeenCalledOnce());
    expect(mock.history.post).toHaveLength(2);
    expect(JSON.parse(mock.history.post[1].data).studentId).toBe(42);
  });

  it('shows search errors and allows retrying the student search', async () => {
    vi.mocked(studentApi.list).mockRejectedValueOnce(new Error('学员服务暂时不可用'));
    show();
    expect(await screen.findByRole('alert')).toHaveTextContent('学员服务暂时不可用');
    await userEvent.click(screen.getByRole('button', { name: '重试搜索' }));
    expect(await screen.findByRole('button', { name: /李小画.*ART-2026-018/ })).toBeInTheDocument();
  });

  it.each([
    ['account', 2, 1],
    ['branch permissions', 1, 2],
  ] as const)(
    'does not reuse cached students after changing %s with the same keyword',
    async (_, nextUserId, nextBranchId) => {
      client.setDefaultOptions({ queries: { retry: false, staleTime: 30_000 } });
      const currentUser = {
        id: 1,
        name: '老师',
        username: 'teacher',
        roles: [],
        permissions: ['student:read'],
        branches: [{ id: 1, name: '一校区', code: 'A' }],
      };
      useAuthStore.getState().setUser(currentUser);
      show();
      await userEvent.type(screen.getByLabelText('搜索学员'), 'ART');
      expect(
        await screen.findByRole('button', { name: /李小画.*ART-2026-018/ }),
      ).toBeInTheDocument();
      vi.mocked(studentApi.list).mockResolvedValue({
        records: [
          { ...student, id: '9223372036854775805', name: '王小画', enrollNo: 'ART-2026-019' },
        ],
        total: 1,
        page: 1,
        size: 20,
      });

      act(() =>
        useAuthStore.getState().setUser({
          ...currentUser,
          id: nextUserId,
          branches: [{ id: nextBranchId, name: '校区', code: 'B' }],
        }),
      );

      expect(
        screen.queryByRole('button', { name: /李小画.*ART-2026-018/ }),
      ).not.toBeInTheDocument();
      expect(
        await screen.findByRole('button', { name: /王小画.*ART-2026-019/ }),
      ).toBeInTheDocument();
      expect(studentApi.list).toHaveBeenLastCalledWith({ keyword: 'ART', page: '1', size: '20' });
    },
  );
});
