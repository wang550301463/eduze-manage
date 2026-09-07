import type { ApiResponse } from '@/lib/api-types';
import { apiClient } from '@/lib/axios';
import type { Branch, PageResult, Role, UserAccount } from './types';

type BackendPage<T> = { records: T[]; total: number; page: number; size: number };

async function getPage<T>(url: string, params: Record<string, unknown>): Promise<PageResult<T>> {
  const { data } = await apiClient.get<ApiResponse<BackendPage<T>>>(url, { params });
  const p = data.data;
  return { items: p.records, total: p.total, page: p.page, size: p.size };
}

export async function listBranches(): Promise<Branch[]> {
  const { data } = await apiClient.get<ApiResponse<Branch[]>>('/branches');
  return data.data;
}

export async function createBranch(body: Partial<Branch>): Promise<Branch> {
  const { data } = await apiClient.post<ApiResponse<Branch>>('/branches', body);
  return data.data;
}

export async function updateBranch(id: number, body: Partial<Branch>): Promise<Branch> {
  const { data } = await apiClient.put<ApiResponse<Branch>>(`/branches/${id}`, body);
  return data.data;
}

export async function deleteBranch(id: number): Promise<void> {
  await apiClient.delete(`/branches/${id}`);
}

export async function listUsers(
  page = 1,
  size = 20,
  keyword?: string,
): Promise<PageResult<UserAccount>> {
  return getPage('/users', { page, size, keyword });
}

export async function createUser(body: Record<string, unknown>): Promise<UserAccount> {
  const { data } = await apiClient.post<ApiResponse<UserAccount>>('/users', body);
  return data.data;
}

export async function updateUser(id: number, body: Record<string, unknown>): Promise<UserAccount> {
  const { data } = await apiClient.put<ApiResponse<UserAccount>>(`/users/${id}`, body);
  return data.data;
}

export async function deleteUser(id: number): Promise<void> {
  await apiClient.delete(`/users/${id}`);
}

export async function assignUserRoles(id: number, roleIds: number[]): Promise<void> {
  await apiClient.post(`/users/${id}/roles`, { roleIds });
}

export async function assignUserBranches(id: number, branchIds: number[]): Promise<void> {
  await apiClient.post(`/users/${id}/branches`, { branchIds });
}

export async function listRoles(): Promise<Role[]> {
  const { data } = await apiClient.get<ApiResponse<Role[]>>('/roles');
  return data.data;
}

export async function createRole(body: { code: string; name: string }): Promise<Role> {
  const { data } = await apiClient.post<ApiResponse<Role>>('/roles', body);
  return data.data;
}

export async function updateRole(id: number, body: { code: string; name: string }): Promise<Role> {
  const { data } = await apiClient.put<ApiResponse<Role>>(`/roles/${id}`, body);
  return data.data;
}

export async function deleteRole(id: number): Promise<void> {
  await apiClient.delete(`/roles/${id}`);
}
