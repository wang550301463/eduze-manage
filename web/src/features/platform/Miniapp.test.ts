import { describe, it, expect, vi } from 'vitest';
import { Session, MiniClient } from '../../../../miniapp/core/client';
describe('native client session isolation', () => {
  it('drops cached private data on role, child and campus switches', () => {
    const s = new Session();
    s.signIn({
      accessToken: 't',
      user: { id: 'u', roles: ['TEACHER'], branches: [{ id: 'b', name: '校区' }] },
    });
    s.cache.set('draft', 'private');
    s.switchScope({ role: 'PARENT', childId: 'a', branchId: 'b' });
    expect(s.cache.size).toBe(0);
    s.cache.set('draft', 'child A');
    s.switchScope({ childId: 'c' });
    expect(s.cache.size).toBe(0);
    s.cache.set('draft', 'campus B');
    s.switchScope({ branchId: 'd' });
    expect(s.cache.size).toBe(0);
  });
  it('rejects late responses from a previous identity before they render', async () => {
    let resolve!: (v: unknown) => void;
    const transport = () =>
      new Promise((resolveFn) => {
        resolve = resolveFn;
      });
    const session = new Session();
    session.signIn({ accessToken: 'a', user: { id: 'A', roles: [], branches: [] } });
    const client = new MiniClient(session, transport);
    const request = client.get('/private');
    session.signIn({ accessToken: 'b', user: { id: 'B', roles: [], branches: [] } });
    resolve({ statusCode: 200, data: { code: 0, data: 'A private' } });
    await expect(request).rejects.toMatchObject({ status: 409 });
  });
  it('keeps draft conflicts visible without an automatic overwrite retry', async () => {
    const transport = vi
      .fn()
      .mockResolvedValue({ statusCode: 409, data: { code: 409, message: '其他设备已更新' } });
    const client = new MiniClient(new Session(), transport);
    await expect(client.post('/records', { version: 1 })).rejects.toMatchObject({ status: 409 });
    expect(transport).toHaveBeenCalledTimes(1);
  });
  it('fails closed and clears auth when a session is revoked', async () => {
    const session = new Session();
    session.signIn({ accessToken: 'a', user: { id: 'A', roles: [], branches: [] } });
    session.cache.set('p', 'private');
    const client = new MiniClient(session, async () => ({
      statusCode: 401,
      data: { code: 401, message: '已失效' },
    }));
    await expect(client.get('/private')).rejects.toMatchObject({ status: 401 });
    expect(session.token).toBe('');
    expect(session.cache.size).toBe(0);
  });
});
