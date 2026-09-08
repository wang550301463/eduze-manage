import { KPICard } from '@/components/ui/KPICard';
import type { DashboardKpis } from '@/features/attendance/types';

type Props = {
  data: DashboardKpis;
};

export function BranchAttendanceKpis({ data }: Props): JSX.Element {
  return (
    <div className="grid grid-cols-2 gap-3 lg:grid-cols-4 lg:gap-4">
      <KPICard title="本周缺勤人次" value={data.weekAbsent} />
      <KPICard title="本月平均出勤率" value={data.monthAttendanceRate} suffix="%" />
      <KPICard title="本周新生" value={data.weekNewStudents} />
      <KPICard title="本周课次" value={data.weekLessons} />
    </div>
  );
}
