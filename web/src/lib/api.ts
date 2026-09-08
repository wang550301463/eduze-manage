import { useAuthStore } from '@/features/auth/store';
import { redirectToLogin, refreshAccessToken } from '@/lib/axios';

const TOKEN_KEY = 'eduze_access_token';

export type ApiEnvelope<T> = {
  code: number;
  message: string;
  data: T;
};

export function getAccessToken(): string | null {
  const fromStore = useAuthStore.getState().accessToken;
  if (fromStore) return fromStore;
  return localStorage.getItem(TOKEN_KEY);
}

export function setAccessToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  if (!headers.has('Content-Type') && init?.body && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }
  const token = getAccessToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  let res = await fetch(path, { ...init, headers });
  if (
    res.status === 401 && token &&
    !path.includes('/auth/login') && !path.includes('/auth/refresh')
  ) {
    // Both HTTP clients share one refresh; a command is retried at most once.
    const refreshed = await refreshAccessToken();
    headers.set('Authorization', `Bearer ${refreshed}`);
    res = await fetch(path, { ...init, headers });
    if (res.status === 401) redirectToLogin();
  }
  const json = (await res.json()) as ApiEnvelope<T>;
  if (!res.ok || json.code !== 0) {
    throw new ApiError(json.message, json.code, json);
  }
  return json.data;
}

export class ApiError extends Error {
  code: number;
  payload: unknown;

  constructor(message: string, code: number, payload: unknown) {
    super(message);
    this.code = code;
    this.payload = payload;
  }
}

const FIELD_LABELS: Record<string, string> = {
  branchId: '校区',
  enrollNo: '入园编号',
  name: '姓名',
  emergencyPhone: '紧急联系电话',
  guardianPhone: '家长手机',
  phone: '家长手机',
};

/** 解析后端校验错误（含 fieldErrors） */
export function formatApiErrorMessage(payload: unknown, fallback = '请求失败'): string {
  if (!payload || typeof payload !== 'object') return fallback;
  const envelope = payload as {
    message?: string;
    data?: { message?: string; fieldErrors?: Record<string, string> };
  };
  const fieldErrors = envelope.data?.fieldErrors;
  if (fieldErrors && Object.keys(fieldErrors).length > 0) {
    return Object.entries(fieldErrors)
      .map(([k, v]) => `${FIELD_LABELS[k] ?? k}：${v}`)
      .join('；');
  }
  return envelope.data?.message ?? envelope.message ?? fallback;
}
