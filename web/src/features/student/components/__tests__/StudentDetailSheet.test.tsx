import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MockAdapter from 'axios-mock-adapter';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '@/features/auth/store';
import { apiClient } from '@/lib/axios';
import { studentApi } from '../../api';
import type { Student } from '../../types';
import { StudentDetailSheet } from '../StudentDetailSheet';

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
const guardianId = '9223372036854775804';

describe('StudentDetailSheet entry points', () => {
  let mock: MockAdapter;
  let client: QueryClient;

  beforeEach(() => {
    mock = new MockAdapter(apiClient);
    client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    vi.spyOn(studentApi, 'get').mockResolvedValue(student);
    useAuthStore.setState({ permissions: ['leave:read', 'guardian:read'] });
    mock.onGet(`/students/${student.id}/guardians`).reply(200, {
      code: 0,
      data: [
        {
          id: guardianId,
          name: '李妈妈',
          phone: '13800000000',
          relation: '母亲',
          canPickup: 1,
          isMainContact: 1,
        },
        {
          id: '2',
          name: '李爸爸',
          phone: '13800000001',
          relation: '父亲',
          canPickup: 1,
          isMainContact: 0,
        },
      ],
    });
  });

  afterEach(() => {
    cleanup();
    mock.restore();
    client.clear();
    vi.restoreAllMocks();
    useAuthStore.getState().clearAuth();
  });

  function show(selectedGuardianId?: string) {
    return render(
      <QueryClientProvider client={client}>
        <StudentDetailSheet
          studentId={student.id}
          guardianId={selectedGuardianId}
          open
          onOpenChange={vi.fn()}
          branches={[]}
        />
      </QueryClientProvider>,
    );
  }

  it('opens the guardians tab and highlights the matching search result', async () => {
    show(guardianId);
    const match = await screen.findByText('李妈妈');
    expect(match.closest('li')).toHaveAttribute('aria-current', 'true');
    expect(screen.getByText('李爸爸').closest('li')).not.toHaveAttribute('aria-current');
    expect(screen.getByText('搜索匹配')).toBeInTheDocument();
  });

  it('passes the loaded student ID to the leave tab', async () => {
    mock.onGet('/leaves').reply(200, { code: 0, data: [] });
    show();
    await userEvent.click(await screen.findByRole('button', { name: '请假' }));
    expect(await screen.findByText('暂无请假记录')).toBeInTheDocument();
    expect(mock.history.get.find((request) => request.url === '/leaves')?.params).toEqual({
      studentId: student.id,
    });
  });
});
