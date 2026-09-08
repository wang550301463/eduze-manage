import type { ApiResponse } from '@/lib/api-types';
import { apiClient } from '@/lib/axios';
import type {
  BulkGenerateResult,
  ConflictReport,
  Lesson,
  LessonChangeLog,
  WeekSchedule,
} from './types';

export async function fetchWeekSchedule(branchId: number, weekStart: string): Promise<WeekSchedule> {
  const { data } = await apiClient.get<ApiResponse<WeekSchedule>>('/schedule/week', {
    params: { branchId, weekStart },
  });
  return data.data;
}

export async function fetchLesson(id: number | string): Promise<Lesson> {
  const { data } = await apiClient.get<ApiResponse<Lesson>>(`/lessons/${id}`);
  return data.data;
}

export async function fetchLessonLogs(id: number | string): Promise<LessonChangeLog[]> {
  const { data } = await apiClient.get<ApiResponse<LessonChangeLog[]>>(`/lessons/${id}/change-logs`);
  return data.data;
}

export async function checkConflict(body: Record<string, unknown>): Promise<ConflictReport> {
  const { data } = await apiClient.post<ApiResponse<ConflictReport>>('/lessons/check-conflict', body);
  return data.data;
}

export async function rescheduleLesson(
  id: number | string,
  body: Record<string, unknown>,
): Promise<Lesson> {
  const { data } = await apiClient.post<ApiResponse<Lesson>>(`/lessons/${id}/reschedule`, body);
  return data.data;
}

export async function cancelLesson(id: number | string, reason: string): Promise<Lesson> {
  const { data } = await apiClient.post<ApiResponse<Lesson>>(`/lessons/${id}/cancel`, { reason });
  return data.data;
}

export async function bulkGenerateLessons(body: Record<string, unknown>): Promise<BulkGenerateResult> {
  const { data } = await apiClient.post<ApiResponse<BulkGenerateResult>>('/lessons/bulk-generate', body);
  return data.data;
}
