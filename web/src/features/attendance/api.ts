import { apiClient } from '@/lib/axios';
import type { ApiResponse } from '@/lib/api-types';
import type { EntityId } from '@/features/student/types';
import type {
  AttendanceRecord,
  DashboardKpis,
  DayPeriod,
  GuardianSummary,
  LeaveRecord,
  StudentAttendanceStat,
  TodayRoster,
} from '@/features/attendance/types';

export async function fetchTodayRoster(params: {
  branchId: number | string;
  period: DayPeriod;
  date?: string;
}): Promise<TodayRoster> {
  const { data } = await apiClient.get<ApiResponse<TodayRoster>>('/attendance/today', { params });
  return data.data;
}

export async function checkIn(body: {
  lessonId?: number | string;
  studentId?: number | string;
  method: 'manual' | 'qr';
  guardianId?: number | string;
  qrCode?: string;
}): Promise<AttendanceRecord> {
  const { data } = await apiClient.post<ApiResponse<AttendanceRecord>>('/attendance/check-in', body);
  return data.data;
}

export async function checkOut(body: {
  attendanceId: number | string;
  guardianId?: number | string;
  isAbnormal?: boolean;
  abnormalNote?: string;
}): Promise<AttendanceRecord> {
  const { data } = await apiClient.post<ApiResponse<AttendanceRecord>>('/attendance/check-out', body);
  return data.data;
}

export async function fetchStudentGuardians(
  studentId: number | string,
  pickupOnly = true,
): Promise<GuardianSummary[]> {
  const { data } = await apiClient.get<ApiResponse<GuardianSummary[]>>(
    `/students/${String(studentId)}/guardians`,
    { params: { pickupOnly } },
  );
  return data.data;
}

export async function fetchLeaves(params?: {
  status?: number;
  studentId?: EntityId | number;
  from?: string;
  to?: string;
}): Promise<LeaveRecord[]> {
  const { data } = await apiClient.get<ApiResponse<LeaveRecord[]>>('/leaves', { params });
  return data.data;
}

export async function createLeave(body: {
  studentId: number | string;
  lessonId?: number | string;
  leaveStartDate: string;
  leaveEndDate: string;
  reason?: string;
}): Promise<LeaveRecord> {
  const { data } = await apiClient.post<ApiResponse<LeaveRecord>>('/leaves', body);
  return data.data;
}

export async function approveLeave(id: number | string): Promise<LeaveRecord> {
  const { data } = await apiClient.post<ApiResponse<LeaveRecord>>(`/leaves/${String(id)}/approve`);
  return data.data;
}

export async function rejectLeave(id: number | string): Promise<LeaveRecord> {
  const { data } = await apiClient.post<ApiResponse<LeaveRecord>>(`/leaves/${String(id)}/reject`);
  return data.data;
}

export async function fetchStudentAttendanceStat(
  studentId: number,
  from?: string,
  to?: string,
): Promise<StudentAttendanceStat> {
  const { data } = await apiClient.get<ApiResponse<StudentAttendanceStat>>(
    `/stats/attendance/student/${studentId}`,
    { params: { from, to } },
  );
  return data.data;
}

export async function fetchDashboardKpis(branchId: number): Promise<DashboardKpis> {
  const { data } = await apiClient.get<ApiResponse<DashboardKpis>>('/stats/attendance/dashboard', {
    params: { branchId },
  });
  return data.data;
}

export async function generateGuardianQr(
  guardianId: number | string,
): Promise<{ guardianId: number | string; qrCode: string }> {
  const { data } = await apiClient.post<ApiResponse<{ guardianId: number | string; qrCode: string }>>(
    `/guardians/${String(guardianId)}/qr`,
  );
  return data.data;
}

export async function fetchStudentAttendanceHistory(
  studentId: number | string,
  from?: string,
  to?: string,
): Promise<AttendanceRecord[]> {
  const { data } = await apiClient.get<ApiResponse<AttendanceRecord[]>>(
    `/attendance/student/${String(studentId)}`,
    { params: { from, to } },
  );
  return data.data;
}
