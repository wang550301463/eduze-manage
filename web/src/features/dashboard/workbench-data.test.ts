import { describe, expect, it } from 'vitest';
import { organizeLessons } from './workbench-data';
import type { TeacherSchedule, LessonCell } from '@/features/teacher/types';
const lesson = (id: string, startAt: string, endAt: string, status = 'scheduled'): LessonCell => ({
  lessonId: id,
  startAt,
  endAt,
  status,
  dayOfWeek: 2,
  startMinute: 600,
  endMinute: 660,
  studentCount: 3,
});
const schedule: TeacherSchedule = {
  weekStart: '2026-09-07',
  columns: [
    {
      teacherId: '1',
      teacherName: '林老师',
      lessons: [
        lesson('9007199254740993', '2026-09-08T14:00:00', '2026-09-08T15:00:00'),
        lesson('2', '2026-09-08T09:00:00', '2026-09-08T10:00:00'),
      ],
    },
    {
      teacherId: '2',
      teacherName: '陈老师',
      lessons: [
        lesson('3', '2026-09-08T13:00:00', '2026-09-08T14:00:00'),
        lesson('4', '2026-09-08T12:00:00', '2026-09-08T13:00:00', 'cancelled'),
      ],
    },
  ],
};
describe('workbench schedule', () => {
  it('sorts all teachers by actual time and excludes cancelled lessons without losing IDs', () => {
    const result = organizeLessons(schedule, new Date('2026-09-08T13:30:00'));
    expect(result.total).toBe(3);
    expect(result.today.map((item) => item.lessonId)).toEqual(['2', '3', '9007199254740993']);
    expect(result.upcoming.map((item) => item.lessonId)).toEqual(['3', '9007199254740993']);
    expect(result.upcoming[0].teacherName).toBe('陈老师');
  });
  it('does not mistake the same weekday in another week for today', () => {
    expect(organizeLessons(schedule, new Date('2026-09-15T09:00:00')).today).toEqual([]);
  });
});
