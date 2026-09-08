import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MockAdapter from 'axios-mock-adapter';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { useAuthStore } from '@/features/auth/store';
import { apiClient } from '@/lib/axios';
import { StudentGuardiansTab } from '../StudentGuardiansTab';

const studentId = '9223372036854775806';
const guardians = [
  {
    id: '9223372036854775805',
    name: '李妈妈',
    phone: '13800000000',
    relation: '母亲',
    canPickup: 1,
    isMainContact: 1,
  },
];

describe('StudentGuardiansTab', () => {
  let mock: MockAdapter;
  let client: QueryClient;

  beforeEach(() => {
    mock = new MockAdapter(apiClient);
    client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    useAuthStore.setState({ permissions: ['guardian:read'] });
  });

  afterEach(() => {
    cleanup();
    client.clear();
    mock.restore();
    useAuthStore.getState().clearAuth();
  });

  function show() {
    render(
      <QueryClientProvider client={client}>
        <StudentGuardiansTab studentId={studentId} />
      </QueryClientProvider>,
    );
  }

  it('does not request or display cached guardian data without guardian read permission', () => {
    useAuthStore.setState({ permissions: [] });
    client.setQueryData(['student-guardians-all', studentId], guardians);
    mock.onGet(`/students/${studentId}/guardians`).reply(200, { code: 0, data: guardians });
    show();

    expect(screen.getByText('暂无查看家长信息的权限')).toBeInTheDocument();
    expect(screen.queryByText('李妈妈')).not.toBeInTheDocument();
    expect(mock.history.get).toHaveLength(0);
  });

  it('shows a request error instead of an empty state and can retry', async () => {
    mock
      .onGet(`/students/${studentId}/guardians`)
      .replyOnce(400, { code: 400, message: '家长信息暂时无法读取' });
    mock.onGet(`/students/${studentId}/guardians`).reply(200, { code: 0, data: guardians });
    show();

    expect(await screen.findByRole('alert')).toHaveTextContent('家长信息暂时无法读取');
    expect(screen.queryByText('暂无家长信息')).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: '重试' }));
    expect(await screen.findByText('李妈妈')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(mock.history.get).toHaveLength(2);
    expect(mock.history.get[1].params).toEqual({ pickupOnly: false });
  });

  it('shows an empty state only after a successful empty response', async () => {
    mock.onGet(`/students/${studentId}/guardians`).reply(200, { code: 0, data: [] });
    show();
    expect(await screen.findByText('暂无家长信息')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
