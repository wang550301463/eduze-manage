import MockAdapter from 'axios-mock-adapter';
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '@/features/auth/store';
import { apiFetch, ApiError } from '@/lib/api';
import { apiClient, rawClient } from '@/lib/axios';

describe('apiFetch session expiry', () => {
  let rawMock: MockAdapter;
  let clientMock: MockAdapter;
  const assign = vi.fn();
  const fetchMock = vi.fn<typeof fetch>();
  const response = (status: number, data: unknown, message = 'OK') =>
    new Response(JSON.stringify({ code: status === 200 ? 0 : status, message, data }), { status });

  beforeAll(() => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { ...window.location, assign },
    });
  });

  beforeEach(() => {
    rawMock = new MockAdapter(rawClient);
    clientMock = new MockAdapter(apiClient);
    useAuthStore.setState({ accessToken: 'expired-access', refreshToken: 'valid-refresh' });
    fetchMock.mockReset();
    assign.mockClear();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    rawMock.restore();
    clientMock.restore();
    useAuthStore.getState().clearAuth();
    vi.unstubAllGlobals();
  });

  it('refreshes an expired student save and replays its body with the new token', async () => {
    const authorizations: (string | null)[] = [];
    fetchMock.mockImplementation(async (_url, init) => {
      authorizations.push(new Headers(init?.headers).get('Authorization'));
      return authorizations.length === 1 ? response(401, null) : response(200, { id: 'student-1' });
    });
    rawMock.onPost('/auth/refresh').reply(200, {
      code: 0, data: { accessToken: 'renewed-access' },
    });
    const body = JSON.stringify({ name: '验收学生' });
    await expect(apiFetch('/api/students', { method: 'POST', body })).resolves.toEqual({ id: 'student-1' });
    expect(authorizations).toEqual(['Bearer expired-access', 'Bearer renewed-access']);
    expect(fetchMock.mock.calls[1][1]?.body).toBe(body);
    expect(rawMock.history.post).toHaveLength(1);
  });

  it('shares one refresh with the existing axios client', async () => {
    fetchMock.mockResolvedValueOnce(response(401, null)).mockResolvedValueOnce(response(200, []));
    clientMock.onGet('/me').replyOnce(401).onGet('/me').reply(200, { code: 0, data: {} });
    rawMock.onPost('/auth/refresh').reply(() => new Promise((resolve) => {
      setTimeout(() => resolve([200, { code: 0, data: { accessToken: 'renewed-access' } }]), 20);
    }));
    await Promise.all([apiFetch('/api/students'), apiClient.get('/me')]);
    expect(rawMock.history.post).toHaveLength(1);
  });

  it('clears expired auth when refresh is rejected without replaying the command', async () => {
    fetchMock.mockResolvedValue(response(401, null));
    rawMock.onPost('/auth/refresh').reply(401);
    await expect(apiFetch('/api/students')).rejects.toBeDefined();
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(useAuthStore.getState().accessToken).toBeNull();
    expect(assign).toHaveBeenCalledWith('/login');
  });

  it('does not loop if the renewed request is also unauthorized', async () => {
    fetchMock.mockImplementation(async () => response(401, null));
    rawMock.onPost('/auth/refresh').reply(200, { code: 0, data: { accessToken: 'renewed-access' } });
    await expect(apiFetch('/api/students')).rejects.toBeInstanceOf(ApiError);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(rawMock.history.post).toHaveLength(1);
    expect(useAuthStore.getState().accessToken).toBeNull();
  });

  it('preserves validation details and does not refresh a business error', async () => {
    const data = { fieldErrors: { enrollNo: '编号已存在' } };
    fetchMock.mockResolvedValue(response(409, data, '请检查编号'));
    await expect(apiFetch('/api/students')).rejects.toMatchObject({
      message: '请检查编号', payload: { code: 409, data },
    });
    expect(rawMock.history.post).toHaveLength(0);
  });
});
