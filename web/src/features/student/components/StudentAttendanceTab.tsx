import { useQuery } from '@tanstack/react-query';
import { format, subDays } from 'date-fns';
import { Skeleton } from '@/components/ui/Skeleton';
import { AttendanceStatusBadge } from '@/features/attendance/components/AttendanceStatusBadge';
import { fetchStudentAttendanceHistory } from '@/features/attendance/api';
import { rosterVisualStatus } from '@/features/attendance/types';

type Props = {
  studentId: string;
};

export function StudentAttendanceTab({ studentId }: Props): JSX.Element {
  const from = format(subDays(new Date(), 30), 'yyyy-MM-dd');
  const to = format(new Date(), 'yyyy-MM-dd');

  const { data = [], isLoading } = useQuery({
    queryKey: ['student-attendance', studentId, from, to],
    queryFn: () => fetchStudentAttendanceHistory(studentId, from, to),
    enabled: Boolean(studentId),
  });

  if (isLoading) {
    return <Skeleton className="h-32 w-full" />;
  }

  if (data.length === 0) {
    return <p className="text-sm text-muted-fg">近 30 天暂无出勤记录</p>;
  }

  return (
    <ul className="divide-y divide-border text-sm">
      {data.map((row) => (
        <li key={row.id} className="flex items-center justify-between py-2">
          <div>
            <p className="font-medium">{row.classGroupName}</p>
            <p className="text-xs text-muted-fg">
              {row.lessonStartAt?.slice(0, 16).replace('T', ' ')}
            </p>
          </div>
          <AttendanceStatusBadge
            status={rosterVisualStatus(row.status)}
            label={row.statusLabel}
          />
        </li>
      ))}
    </ul>
  );
}
