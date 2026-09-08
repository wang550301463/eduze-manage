import type { UploadTicket } from '../../../../packages/contracts';
import { assertMediaLimit, publicationMediaIds } from '../../../../packages/contracts/media';
import type { MediaUploadPurpose } from '../../../../packages/contracts/media';
import type { DraftInput } from './types';
import { useAuthStore } from '@/features/auth/store';
import { redirectToLogin, refreshAccessToken } from '@/lib/axios';

export class PlatformError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly path?: string,
  ) {
    super(message);
    this.name = 'PlatformError';
  }
}

export async function platformRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  let token = useAuthStore.getState().accessToken;
  const send = () => fetch(`/api/v1${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init.headers,
    },
  });
  let response = await send();
  if (token !== useAuthStore.getState().accessToken)
    throw new PlatformError(409, '登录身份已变化，请重新打开页面');
  if (response.status === 401 && token) {
    token = await refreshAccessToken();
    response = await send();
  }
  const body = await response.json();
  if (token !== useAuthStore.getState().accessToken)
    throw new PlatformError(409, '登录身份已变化，请重新打开页面');
  if (response.status === 401) redirectToLogin();
  if (!response.ok || body.code !== 0)
    throw new PlatformError(response.status, body.message || '服务暂不可用，请重试', path);
  return body.data as T;
}
export const command = <T>(path: string, data: unknown, method = 'POST') => {
  if (
    path.startsWith('/portfolio/') &&
    data &&
    typeof data === 'object' &&
    'artworks' in data &&
    'audioMediaIds' in data
  )
    assertMediaLimit(publicationMediaIds([data as DraftInput]));
  return platformRequest<T>(path, { method, body: JSON.stringify(data) });
};

export async function uploadMedia(file: File, branchId: string, purpose: MediaUploadPurpose): Promise<string> {
  const ticket = await command<UploadTicket>('/media/uploads', {
    branchId,
    purpose,
    fileName: file.name,
    contentType: file.type || 'application/octet-stream',
    size: file.size,
  });
  const result = await fetch(ticket.uploadUrl, {
    method: ticket.method,
    headers: ticket.headers,
    body: file,
  });
  if (!result.ok) throw new PlatformError(result.status, '上传失败，请重试此文件');
  await command(`/media/uploads/${ticket.id}/complete`, {});
  return ticket.id;
}
