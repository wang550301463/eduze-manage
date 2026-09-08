import { describe, expect, it, vi, afterEach } from 'vitest';
import { uploadMedia, PlatformError, platformRequest } from './client';
import { useAuthStore } from '@/features/auth/store';
import MockAdapter from 'axios-mock-adapter';
import { rawClient } from '@/lib/axios';

afterEach(() => vi.unstubAllGlobals());
describe('platform transport', () => {
  it('renews an expired teaching request once instead of logging the user out', async () => {
    useAuthStore.setState({ accessToken: 'expired', refreshToken: 'refresh' });
    const mock = new MockAdapter(rawClient);
    mock.onPost('/auth/refresh').reply(200, { code: 0, data: { accessToken: 'renewed' } });
    const fetcher = vi.fn()
      .mockResolvedValueOnce({ ok: false, status: 401, json: async () => ({ code: 401 }) })
      .mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ code: 0, data: ['theme'] }) });
    vi.stubGlobal('fetch', fetcher);
    try {
      await expect(platformRequest('/teaching/themes')).resolves.toEqual(['theme']);
      expect(fetcher).toHaveBeenCalledTimes(2);
      expect(new Headers(fetcher.mock.calls[1][1].headers).get('Authorization')).toBe('Bearer renewed');
      expect(mock.history.post).toHaveLength(1);
    } finally { mock.restore(); useAuthStore.getState().clearAuth(); }
  });
  it('keeps optimistic conflicts distinguishable and never retries a write', async () => {
    const fetcher = vi.fn().mockResolvedValue({
      ok: false,
      status: 409,
      json: async () => ({ code: 409, message: '草稿已更新' }),
    });
    vi.stubGlobal('fetch', fetcher);
    await expect(
      platformRequest('/portfolio/drafts/1', { method: 'PUT', body: '{}' }),
    ).rejects.toMatchObject({ status: 409 });
    expect(fetcher).toHaveBeenCalledTimes(1);
  });
  it('never completes an upload rejected by object storage', async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 0,
          data: { id: '1', uploadUrl: 'https://oss.example/a', method: 'PUT', headers: {} },
        }),
      })
      .mockResolvedValueOnce({ ok: false, status: 503 });
    vi.stubGlobal('fetch', fetcher);
    await expect(
      uploadMedia(new File(['photo'], 'a.jpg', { type: 'image/jpeg' }), '1', 'ARTWORK'),
    ).rejects.toThrow('上传失败');
    expect(fetcher).toHaveBeenCalledTimes(2);
  });
  it('never leaks the login token into a signed object storage request', async () => {
    useAuthStore.setState({ accessToken: 'secret' });
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 0,
          data: {
            id: '1',
            uploadUrl: 'https://oss.example/a',
            method: 'PUT',
            headers: { 'Content-Type': 'image/jpeg' },
          },
        }),
      })
      .mockResolvedValueOnce({ ok: true })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ code: 0, data: { id: '1', state: 'READY' } }),
      });
    vi.stubGlobal('fetch', fetcher);
    await uploadMedia(new File(['photo'], 'a.jpg', { type: 'image/jpeg' }), '1', 'ARTWORK');
    expect(fetcher.mock.calls[1][1].headers).not.toHaveProperty('Authorization');
  });
  it('discards late responses after switching accounts', async () => {
    useAuthStore.setState({ accessToken: 'first' });
    let resolve!: (value: unknown) => void;
    vi.stubGlobal(
      'fetch',
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    );
    const pending = platformRequest('/portfolio/records');
    useAuthStore.setState({ accessToken: 'second' });
    resolve({
      ok: true,
      status: 200,
      json: async () => ({ code: 0, data: ['first account child'] }),
    });
    await expect(pending).rejects.toMatchObject({ status: 409 });
  });
  it('provides actionable errors without requiring backend trace details', () => {
    expect(new PlatformError(409, 'changed').message).toBe('changed');
  });
});
