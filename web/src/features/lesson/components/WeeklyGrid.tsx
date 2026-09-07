import { parseISO } from 'date-fns';
import { LessonCell } from '@/features/lesson/components/LessonCell';
import type { ScheduleLessonItem, WeekSchedule } from '@/features/lesson/types';

const START_HOUR = 8;
const END_HOUR = 22;
const SLOT_MINUTES = 30;
const ROWS = ((END_HOUR - START_HOUR) * 60) / SLOT_MINUTES;

type Props = {
  schedule: WeekSchedule;
  onLessonClick: (lesson: ScheduleLessonItem) => void;
};

function slotIndex(iso: string): number {
  const d = parseISO(iso);
  const minutes = d.getHours() * 60 + d.getMinutes() - START_HOUR * 60;
  return Math.max(0, Math.floor(minutes / SLOT_MINUTES));
}

function slotSpan(startIso: string, endIso: string): number {
  const start = slotIndex(startIso);
  const end = slotIndex(endIso);
  return Math.max(1, end - start);
}

export function WeeklyGrid({ schedule, onLessonClick }: Props): JSX.Element {
  const hours = Array.from({ length: ROWS }, (_, i) => {
    const total = START_HOUR * 60 + i * SLOT_MINUTES;
    const h = Math.floor(total / 60);
    const m = total % 60;
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
  });

  return (
    <div className="hidden overflow-x-auto md:block">
      <div
        className="relative min-w-[900px] grid gap-px bg-border"
        style={{
          gridTemplateColumns: '80px repeat(7, 1fr)',
          gridTemplateRows: `repeat(${ROWS}, 32px)`,
        }}
      >
        {hours.map((label, row) => (
          <div
            key={label}
            className="bg-background px-2 text-xs text-muted-fg"
            style={{ gridColumn: 1, gridRow: row + 1 }}
          >
            {row % 2 === 0 ? label : ''}
          </div>
        ))}
        {schedule.days.map((day, col) =>
          day.lessons.map((lesson) => {
            const rowStart = slotIndex(lesson.startAt) + 1;
            const rowEnd = rowStart + slotSpan(lesson.startAt, lesson.endAt);
            const weekday = col + 2;
            return (
              <div
                key={lesson.id}
                className="relative bg-background"
                style={{
                  gridColumn: weekday,
                  gridRow: `${rowStart} / ${rowEnd}`,
                }}
              >
                <LessonCell lesson={lesson} onClick={() => onLessonClick(lesson)} />
              </div>
            );
          }),
        )}
      </div>
    </div>
  );
}
