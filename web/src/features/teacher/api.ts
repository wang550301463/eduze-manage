import { apiFetch } from '@/lib/api';
import type {
  EntityId,
  LessonStudentRow,
  Subscription,
  Teacher,
  TeacherAvailability,
  TeacherSchedule,
} from './types';

const sid = (id: EntityId | number) => String(id);

export const teacherApi = {
  list: (branchId?: EntityId | number) =>
    apiFetch<Teacher[]>(`/api/teachers${branchId ? `?branchId=${branchId}` : ''}`),
  listAvailabilities: (teacherId: EntityId) =>
    apiFetch<TeacherAvailability[]>(`/api/teachers/${sid(teacherId)}/availabilities`),
  createAvailability: (teacherId: EntityId, body: unknown) =>
    apiFetch<TeacherAvailability>(`/api/teachers/${sid(teacherId)}/availabilities`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  updateAvailability: (id: EntityId, body: unknown) =>
    apiFetch<TeacherAvailability>(`/api/teacher-availabilities/${sid(id)}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  deleteAvailability: (id: EntityId) =>
    apiFetch<void>(`/api/teacher-availabilities/${sid(id)}`, { method: 'DELETE' }),
};

export const scheduleApi = {
  byTeacher: (branchId?: EntityId, weekStart?: string) => {
    const q = new URLSearchParams();
    if (branchId) q.set('branchId', String(branchId));
    if (weekStart) q.set('weekStart', weekStart);
    const s = q.toString();
    return apiFetch<TeacherSchedule>(`/api/schedule/by-teacher${s ? `?${s}` : ''}`);
  },
  myWeek: (weekStart?: string) => {
    const q = new URLSearchParams();
    if (weekStart) q.set('weekStart', weekStart);
    const s = q.toString();
    return apiFetch<TeacherSchedule>(`/api/schedule/my-week${s ? `?${s}` : ''}`);
  },
  bulkGenerate: (body: unknown) =>
    apiFetch<{ generated: number; skipped: number; rosterAdded: number }>(
      '/api/lessons/bulk-generate',
      { method: 'POST', body: JSON.stringify(body) },
    ),
  lessonStudents: (lessonId: EntityId) =>
    apiFetch<LessonStudentRow[]>(`/api/lessons/${sid(lessonId)}/students`),
  addLessonStudent: (lessonId: EntityId, body: unknown) =>
    apiFetch<LessonStudentRow>(`/api/lessons/${sid(lessonId)}/students`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  removeLessonStudent: (lessonId: EntityId, studentId: EntityId) =>
    apiFetch<void>(`/api/lessons/${sid(lessonId)}/students/${sid(studentId)}`, {
      method: 'DELETE',
    }),
};

export const subscriptionApi = {
  list: (params: { studentId?: EntityId; teacherId?: EntityId }) => {
    const q = new URLSearchParams();
    if (params.studentId) q.set('studentId', String(params.studentId));
    if (params.teacherId) q.set('teacherId', String(params.teacherId));
    const s = q.toString();
    return apiFetch<Subscription[]>(`/api/subscriptions${s ? `?${s}` : ''}`);
  },
  create: (body: unknown) =>
    apiFetch<Subscription>('/api/subscriptions', { method: 'POST', body: JSON.stringify(body) }),
  update: (id: EntityId, body: unknown) =>
    apiFetch<Subscription>(`/api/subscriptions/${sid(id)}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  remove: (id: EntityId) =>
    apiFetch<void>(`/api/subscriptions/${sid(id)}`, { method: 'DELETE' }),
};
