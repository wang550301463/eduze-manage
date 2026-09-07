import type { StudentAttendanceStat } from '@/features/attendance/types';

type Props = {
  title: string;
  stat: StudentAttendanceStat;
};

export function ClassAttendanceCard({ title, stat }: Props): JSX.Element {
  return (
    <div className="rounded-lg border border-border p-4">
      <p className="font-medium">{title}</p>
      <p className="mt-2 text-2xl font-semibold text-primary">{stat.rate}%</p>
      <p className="mt-1 text-xs text-muted-fg">
        出勤 {stat.present} · 缺勤 {stat.absent} · 请假 {stat.leave}
      </p>
    </div>
  );
}
