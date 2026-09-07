import { useQuery } from '@tanstack/react-query';
import { format } from 'date-fns';
import { Skeleton } from '@/components/ui/Skeleton';
import { AttendanceRosterTable } from '@/features/attendance/components/AttendanceRosterTable';
import { fetchTodayRoster } from '@/features/attendance/api';
import type { RosterItem } from '@/features/attendance/types';

type Props = {
  branchId: number;
  lessonStartAt: string;
  readOnly?: boolean;
  onCheckIn?: (item: RosterItem) => void;
  onCheckOut?: (item: RosterItem) => void;
};

export function LessonRosterTable({
  branchId,
  lessonStartAt,
  readOnly = true,
  onCheckIn = () => undefined,
  onCheckOut = () => undefined,
}: Props): JSX.Element {
  const date = lessonStartAt.slice(0, 10);
  const hour = Number(lessonStartAt.slice(11, 13));
  const period = hour < 12 ? 'morning' : hour < 18 ? 'afternoon' : 'evening';

  const { data, isLoading } = useQuery({
    queryKey: ['lesson-roster', branchId, date, period],
    queryFn: () => fetchTodayRoster({ branchId, period, date }),
  });

  const items =
    data?.items.filter((i) => i.lessonStartAt.startsWith(lessonStartAt.slice(0, 16))) ?? [];

  if (isLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  return (
    <div className="space-y-2">
      <p className="text-xs text-muted-fg">
        课次 {format(new Date(lessonStartAt), 'yyyy-MM-dd HH:mm')} · {items.length} 人
      </p>
      <AttendanceRosterTable
        items={items}
        readOnly={readOnly}
        onCheckIn={onCheckIn}
        onCheckOut={onCheckOut}
      />
    </div>
  );
}
