import axios from 'axios';
import type { ApiResponse } from '@/lib/api-types';
import { apiClient } from '@/lib/axios';
import type { LoginRequest, LoginResult, UserInfo } from './types';

const rawClient = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

export async function login(req: LoginRequest): Promise<LoginResult> {
  const { data } = await apiClient.post<ApiResponse<LoginResult>>('/auth/login', req);
  return data.data;
}

export async function refresh(refreshToken: string): Promise<LoginResult> {
  const { data } = await rawClient.post<ApiResponse<LoginResult>>('/auth/refresh', {
    refreshToken,
  });
  return data.data;
}

export async function logout(): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/logout');
}

export async function getMe(): Promise<UserInfo> {
  const { data } = await apiClient.get<ApiResponse<UserInfo>>('/me');
  return data.data;
}
