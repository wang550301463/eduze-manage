import type { ApiResponse } from '@/lib/api-types';
import { apiClient } from '@/lib/axios';
import type { ClassGroup, ClassMember, ClassRoom, Course, PageResult } from './types';

type BackendPage<T> = { records: T[]; total: number; page: number; size: number };

async function getPage<T>(url: string, params: Record<string, unknown>): Promise<PageResult<T>> {
  const { data } = await apiClient.get<ApiResponse<BackendPage<T>>>(url, { params });
  const p = data.data;
  return { items: p.records, total: p.total, page: p.page, size: p.size };
}

export async function listCourses(page = 1, size = 20, keyword?: string): Promise<PageResult<Course>> {
  return getPage('/courses', { page, size, keyword });
}

export async function createCourse(body: Partial<Course>): Promise<Course> {
  const { data } = await apiClient.post<ApiResponse<Course>>('/courses', body);
  return data.data;
}

export async function updateCourse(id: string, body: Partial<Course>): Promise<Course> {
  const { data } = await apiClient.put<ApiResponse<Course>>(`/courses/${id}`, body);
  return data.data;
}

export async function deleteCourse(id: string): Promise<void> {
  await apiClient.delete(`/courses/${id}`);
}

export async function listClassGroups(
  page = 1,
  size = 20,
  branchId?: number | string,
  courseId?: number | string,
): Promise<PageResult<ClassGroup>> {
  return getPage('/class-groups', { page, size, branchId, courseId });
}

export async function getClassGroup(id: number | string): Promise<ClassGroup> {
  const { data } = await apiClient.get<ApiResponse<ClassGroup>>(`/class-groups/${id}`);
  return data.data;
}

export async function createClassGroup(body: Record<string, unknown>): Promise<ClassGroup> {
  const { data } = await apiClient.post<ApiResponse<ClassGroup>>('/class-groups', body);
  return data.data;
}

export async function updateClassGroup(id: number | string, body: Record<string, unknown>): Promise<ClassGroup> {
  const { data } = await apiClient.put<ApiResponse<ClassGroup>>(`/class-groups/${id}`, body);
  return data.data;
}

export async function deleteClassGroup(id: number | string): Promise<void> {
  await apiClient.delete(`/class-groups/${id}`);
}

export async function listClassMembers(classGroupId: number | string): Promise<ClassMember[]> {
  const { data } = await apiClient.get<ApiResponse<ClassMember[]>>(
    `/class-groups/${classGroupId}/members`,
  );
  return data.data;
}

export async function addClassMembers(classGroupId: number | string, studentIds: Array<number | string>): Promise<void> {
  await apiClient.post(`/class-groups/${classGroupId}/members`, { studentIds });
}

export async function removeClassMember(classGroupId: number | string, studentId: number | string): Promise<void> {
  await apiClient.delete(`/class-groups/${classGroupId}/members/${studentId}`);
}

export async function listClassRooms(
  page = 1,
  size = 20,
  branchId?: number,
): Promise<PageResult<ClassRoom>> {
  return getPage('/class-rooms', { page, size, branchId });
}

export async function createClassRoom(body: Record<string, unknown>): Promise<ClassRoom> {
  const { data } = await apiClient.post<ApiResponse<ClassRoom>>('/class-rooms', body);
  return data.data;
}

export async function updateClassRoom(id: number, body: Record<string, unknown>): Promise<ClassRoom> {
  const { data } = await apiClient.put<ApiResponse<ClassRoom>>(`/class-rooms/${id}`, body);
  return data.data;
}

export async function deleteClassRoom(id: number): Promise<void> {
  await apiClient.delete(`/class-rooms/${id}`);
}
