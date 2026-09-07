import { useQuery } from '@tanstack/react-query';
import { format, parseISO, startOfWeek } from 'date-fns';
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { LessonDetailSheet } from '@/features/lesson/components/LessonDetailSheet';
import { WeekNavigator } from '@/features/lesson/components/WeekNavigator';
import { WeeklyGrid } from '@/features/lesson/components/WeeklyGrid';
import { useDefaultBranchId } from '@/features/course/hooks/useDefaultBranchId';
import { fetchWeekSchedule } from '@/features/lesson/api';
import type { ScheduleLessonItem } from '@/features/lesson/types';

export function WeeklySchedulePage(): JSX.Element {
  const branchId = useDefaultBranchId();
  const [searchParams, setSearchParams] = useSearchParams();
  const [weekStart, setWeekStart] = useState(() =>
    startOfWeek(new Date(), { weekStartsOn: 1 }),
  );
  const [selectedLessonId, setSelectedLessonId] = useState<string | null>(null);
  const [sheetOpen, setSheetOpen] = useState(false);

  const weekStartStr = format(weekStart, 'yyyy-MM-dd');

  const { data, refetch } = useQuery({
    queryKey: ['schedule-week', branchId, weekStartStr],
    queryFn: () => fetchWeekSchedule(branchId, weekStartStr),
  });

  useEffect(() => {
    const openLessonId = searchParams.get('openLessonId');
    if (openLessonId) {
      setSelectedLessonId(openLessonId);
      setSheetOpen(true);
    }
    const date = searchParams.get('date');
    if (date) {
      setWeekStart(parseISO(date));
    }
  }, [searchParams]);

  const openLesson = (lesson: ScheduleLessonItem) => {
    setSelectedLessonId(String(lesson.id));
    setSheetOpen(true);
  };

  return (
    <div className="space-y-6" data-testid="weekly-schedule-page">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-2xl font-semibold tracking-tight">周课表</h1>
        <WeekNavigator weekStart={weekStart} onChange={setWeekStart} />
      </div>

      {data ? (
        <div className="overflow-hidden rounded-xl border border-border bg-white">
          <WeeklyGrid schedule={data} onLessonClick={openLesson} />
        </div>
      ) : null}

      <div className="space-y-4 md:hidden">
        {(data?.days ?? []).map((day) => (
          <section
            key={day.date}
            className="overflow-hidden rounded-xl border border-border bg-white p-3"
          >
            <h2 className="mb-2 text-sm font-medium">{day.date}</h2>
            <ul className="space-y-2">
              {day.lessons.map((lesson) => (
                <li key={lesson.id}>
                  <button
                    type="button"
                    className="w-full rounded-lg border border-border p-3 text-left text-sm hover:bg-muted/40"
                    style={{ borderLeftColor: lesson.color, borderLeftWidth: 4 }}
                    onClick={() => openLesson(lesson)}
                  >
                    <div className="font-medium">{lesson.classGroupName}</div>
                    <div className="text-muted-fg">
                      {lesson.startAt.slice(11, 16)}–{lesson.endAt.slice(11, 16)} · {lesson.teacherShortName}
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>

      <LessonDetailSheet
        lessonId={selectedLessonId}
        open={sheetOpen}
        onOpenChange={(open) => {
          setSheetOpen(open);
          if (!open) {
            setSelectedLessonId(null);
            const next = new URLSearchParams(searchParams);
            next.delete('openLessonId');
            next.delete('date');
            setSearchParams(next, { replace: true });
          }
        }}
        onUpdated={() => void refetch()}
      />
    </div>
  );
}
