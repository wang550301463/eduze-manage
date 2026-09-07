import type { ScheduleLessonItem } from '@/features/lesson/types';
import { cn } from '@/lib/cn';

type Props = {
  lesson: ScheduleLessonItem;
  onClick: () => void;
};

export function LessonCell({ lesson, onClick }: Props): JSX.Element {
  const cancelled = lesson.status === 'CANCELLED';
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'absolute inset-x-0.5 overflow-hidden rounded-md border px-1 py-0.5 text-left text-[10px] leading-tight shadow-sm',
        cancelled && 'opacity-50 line-through',
      )}
      style={{
        backgroundColor: `${lesson.color}22`,
        borderColor: lesson.color,
      }}
    >
      <div className="font-medium">{lesson.classGroupName}</div>
      <div className="text-muted-fg">
        {lesson.teacherShortName} · {lesson.classRoomShortName}
      </div>
    </button>
  );
}
