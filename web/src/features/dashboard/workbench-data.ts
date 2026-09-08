import type { TeacherSchedule, LessonCell } from '@/features/teacher/types';
export type WorkbenchLesson = LessonCell & { teacherName: string };

export function organizeLessons(
  schedule: TeacherSchedule | undefined,
  now: Date,
): { today: WorkbenchLesson[]; upcoming: WorkbenchLesson[]; total: number } {
  const lessons = (schedule?.columns ?? [])
    .flatMap((column) =>
      column.lessons.map((lesson) => ({ ...lesson, teacherName: column.teacherName })),
    )
    .filter((lesson) => lesson.status.toLowerCase() !== 'cancelled')
    .sort((a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime());
  return {
    total: lessons.length,
    today: lessons.filter(
      (lesson) => new Date(lesson.startAt).toDateString() === now.toDateString(),
    ),
    upcoming: lessons.filter(
      (lesson) =>
        new Date(lesson.endAt).getTime() > now.getTime() &&
        lesson.status.toLowerCase() !== 'completed',
    ),
  };
}
