import { apiFetch } from '@/lib/api';
import type {
  CoursePackage,
  EntityId,
  Guardian,
  LessonHourLedger,
  MentorHistoryItem,
  PageResult,
  StageAssessment,
  Student,
  StudentLessonHistory,
  TeacherSummary,
} from './types';

function sid(id: EntityId | number): string {
  return String(id);
}

export type StudentListParams = {
  keyword?: string;
  branchId?: string;
  classGroupId?: string;
  status?: string;
  pkgRemainingMax?: string;
  page?: string;
  size?: string;
};

function qs(params: Record<string, string | undefined>): string {
  const q = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v) q.set(k, v);
  });
  const s = q.toString();
  return s ? `?${s}` : '';
}

export const studentApi = {
  list: (params: StudentListParams) =>
    apiFetch<PageResult<Student>>(`/api/students${qs(params)}`),
  get: (id: EntityId) => apiFetch<Student>(`/api/students/${sid(id)}`),
  create: (body: unknown) =>
    apiFetch<Student>('/api/students', { method: 'POST', body: JSON.stringify(body) }),
  update: (id: EntityId, body: unknown) =>
    apiFetch<Student>(`/api/students/${sid(id)}`, { method: 'PUT', body: JSON.stringify(body) }),
  updateStatus: (id: EntityId, status: number) =>
    apiFetch<Student>(`/api/students/${sid(id)}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    }),
  remove: (id: EntityId) =>
    apiFetch<void>(`/api/students/${sid(id)}`, { method: 'DELETE' }),
  guardians: (id: EntityId) => apiFetch<Guardian[]>(`/api/students/${sid(id)}/guardians`),
  upsertGuardian: (id: EntityId, body: unknown) =>
    apiFetch<Guardian>(`/api/students/${sid(id)}/guardians/upsert`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  unlinkGuardian: (studentId: EntityId, guardianId: EntityId) =>
    apiFetch<void>(`/api/students/${sid(studentId)}/guardians/${sid(guardianId)}`, {
      method: 'DELETE',
    }),
  packages: (id: EntityId) => apiFetch<CoursePackage[]>(`/api/students/${sid(id)}/packages`),
  createPackage: (id: EntityId, body: unknown) =>
    apiFetch<CoursePackage>(`/api/students/${sid(id)}/packages`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  updatePackage: (id: EntityId, body: unknown) =>
    apiFetch<CoursePackage>(`/api/packages/${sid(id)}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  deletePackage: (id: EntityId) =>
    apiFetch<void>(`/api/packages/${sid(id)}`, { method: 'DELETE' }),
  searchGuardians: (phone: string) =>
    apiFetch<PageResult<Guardian>>(`/api/guardians?phone=${encodeURIComponent(phone)}&size=10`),
  bulkAssignClass: (body: { studentIds: EntityId[]; classGroupId: EntityId }) =>
    apiFetch<void>('/api/students/bulk/assign-class', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  bulkTransferClass: (body: {
    studentIds: EntityId[];
    fromClassGroupId: EntityId;
    toClassGroupId: EntityId;
  }) =>
    apiFetch<void>('/api/students/bulk/transfer-class', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  changeMentor: (
    id: EntityId,
    body: { toTeacherId: EntityId | number; reason?: string; keepSubscriptions?: boolean },
  ) =>
    apiFetch<void>(`/api/students/${sid(id)}/mentor`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  mentorHistory: (id: EntityId) =>
    apiFetch<MentorHistoryItem[]>(`/api/students/${sid(id)}/mentor-history`),
  createAssessment: (id: EntityId, body: unknown) =>
    apiFetch<StageAssessment>(`/api/students/${sid(id)}/stage-assessments`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  listAssessments: (id: EntityId) =>
    apiFetch<StageAssessment[]>(`/api/students/${sid(id)}/stage-assessments`),
  listLessonHourLedger: (id: EntityId) =>
    apiFetch<LessonHourLedger[]>(`/api/students/${sid(id)}/lesson-hour-ledger`),
  listLessonHistories: (params: {
    studentId?: string;
    branchId?: string;
    from?: string;
    to?: string;
    page?: string;
    size?: string;
  }) =>
    apiFetch<PageResult<StudentLessonHistory>>(
      `/api/student-lesson-histories${qs(params)}`,
    ),
};

export const teacherApi = {
  list: (branchId?: EntityId | number) =>
    apiFetch<TeacherSummary[]>(
      `/api/teachers${branchId ? `?branchId=${branchId}` : ''}`,
    ),
};

export const branchApi = {
  /** 后端返回 List，非分页 PageResult */
  list: () => apiFetch<{ id: string; name: string; code: string }[]>('/api/branches'),
};
