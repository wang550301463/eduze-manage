import axios, {
  type AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from 'axios';
import { useAuthStore } from '@/features/auth/store';
import type { LoginResult } from '@/features/auth/types';
import { ApiError, type ApiResponse } from '@/lib/api-types';
import { toast } from '@/lib/toast';

/** @internal used by unit tests to mock refresh */
export const rawClient = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

type RetryConfig = InternalAxiosRequestConfig & { _retry?: boolean };

let refreshPromise: Promise<string> | null = null;

function redirectToLogin(): void {
  useAuthStore.getState().clearAuth();
  const path = window.location.pathname;
  if (path !== '/login') {
    window.location.assign('/login');
  }
}

async function doRefresh(): Promise<string> {
  const { refreshToken, setAuth, setAccessToken } = useAuthStore.getState();
  if (!refreshToken) {
    throw new Error('No refresh token');
  }
  const { data: body } = await rawClient.post<ApiResponse<LoginResult>>('/auth/refresh', {
    refreshToken,
  });
  if (body.code !== 0) {
    throw new ApiError(body.code, body.message ?? '刷新失败');
  }
  const result = body.data;
  if (result.refreshToken && result.user) {
    setAuth({
      accessToken: result.accessToken,
      refreshToken: result.refreshToken,
      user: result.user,
    });
  } else {
    setAccessToken(result.accessToken);
  }
  return result.accessToken;
}

function enqueueRefresh(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = doRefresh()
      .catch((err) => {
        redirectToLogin();
        throw err;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

function unwrapApiBody<T>(data: unknown): T {
  const body = data as { code?: number; message?: string; data?: T };
  if (body && typeof body.code === 'number' && body.code !== 0) {
    throw new ApiError(body.code, body.message ?? '请求失败');
  }
  return body as T;
}

export const apiClient: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => {
    const contentType = response.headers['content-type'] as string | undefined;
    if (contentType?.includes('application/json') && response.data && typeof response.data === 'object') {
      const body = response.data as { code?: number; message?: string };
      if (typeof body.code === 'number' && body.code !== 0) {
        throw new ApiError(body.code, body.message ?? '请求失败');
      }
    }
    return response;
  },
  async (error: AxiosError<{ code?: number; message?: string }>) => {
    const config = error.config as RetryConfig | undefined;
    const status = error.response?.status;
    const url = config?.url ?? '';

    if (status === 401 && url && !url.includes('/auth/refresh') && !url.includes('/auth/login')) {
      if (config && !config._retry) {
        config._retry = true;
        try {
          const newToken = await enqueueRefresh();
          config.headers.Authorization = `Bearer ${newToken}`;
          return apiClient.request(config);
        } catch {
          return Promise.reject(error);
        }
      }
      redirectToLogin();
      return Promise.reject(error);
    }

    if (status === 403) {
      toast.error('没有权限');
      return Promise.reject(error);
    }

    const bizCode = error.response?.data?.code;
    const bizMessage = error.response?.data?.message;
    if (bizCode !== undefined && bizCode !== 0) {
      return Promise.reject(new ApiError(bizCode, bizMessage ?? '请求失败'));
    }

    if (!error.response || (status !== undefined && status >= 500)) {
      toast.error(bizMessage ?? '服务异常，请稍后再试');
    }

    return Promise.reject(error);
  },
);

export { unwrapApiBody };
