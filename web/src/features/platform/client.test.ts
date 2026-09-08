import { describe, expect, it, vi, afterEach } from 'vitest';
import { uploadMedia, PlatformError, platformRequest } from './client';
import { useAuthStore } from '@/features/auth/store';

afterEach(() => vi.unstubAllGlobals());
describe('platform transport', () => {
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
      uploadMedia(new File(['photo'], 'a.jpg', { type: 'image/jpeg' }), '1', 'PORTFOLIO'),
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
    await uploadMedia(new File(['photo'], 'a.jpg', { type: 'image/jpeg' }), '1', 'PORTFOLIO');
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
