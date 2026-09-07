import MockAdapter from 'axios-mock-adapter';
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '@/features/auth/store';
import { apiClient, rawClient } from '@/lib/axios';

describe('apiClient interceptors', () => {
  let mock: MockAdapter;
  let rawMock: MockAdapter;
  const assignSpy = vi.fn();

  beforeAll(() => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { ...window.location, assign: assignSpy },
    });
  });

  beforeEach(() => {
    mock = new MockAdapter(apiClient);
    rawMock = new MockAdapter(rawClient);
    useAuthStore.setState({
      accessToken: 'old-access',
      refreshToken: 'old-refresh',
      user: {
        id: 1,
        name: 'Test',
        username: 'admin',
        roles: ['SUPER_ADMIN'],
        permissions: ['student:read'],
        branches: [{ id: 1, name: '总部', code: 'HQ' }],
      },
      branchIds: [1],
      permissions: ['student:read'],
    });
    assignSpy.mockClear();
  });

  afterEach(() => {
    mock.restore();
    rawMock.restore();
    useAuthStore.getState().clearAuth();
  });

  it('401 触发 refresh 一次，成功后重放原请求', async () => {
    mock.onGet('/me').replyOnce(401).onGet('/me').reply(200, {
      code: 0,
      message: 'OK',
      data: { id: 1, name: 'Test', username: 'admin', roles: [], permissions: [], branches: [] },
    });
    rawMock.onPost('/auth/refresh').reply(200, {
      code: 0,
      message: 'OK',
      data: {
        accessToken: 'new-access',
        refreshToken: 'old-refresh',
        user: {
          id: 1,
          name: 'Test',
          username: 'admin',
          roles: [],
          permissions: [],
          branches: [],
        },
      },
    });

    const res = await apiClient.get('/me');
    expect(res.status).toBe(200);
    expect(rawMock.history.post.filter((r) => r.url === '/auth/refresh')).toHaveLength(1);
    expect(useAuthStore.getState().accessToken).toBe('new-access');
    expect(assignSpy).not.toHaveBeenCalled();
  });

  it('refresh 失败时清空 store 并跳转 /login', async () => {
    mock.onGet('/protected').reply(401);
    rawMock.onPost('/auth/refresh').reply(401, {
      code: 401,
      message: 'Unauthorized',
      data: null,
    });

    await expect(apiClient.get('/protected')).rejects.toBeDefined();
    expect(useAuthStore.getState().accessToken).toBeNull();
    expect(assignSpy).toHaveBeenCalledWith('/login');
  });
});
