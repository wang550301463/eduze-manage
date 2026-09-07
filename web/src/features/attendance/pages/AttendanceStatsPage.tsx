import { useQuery } from '@tanstack/react-query';
import { format, subWeeks } from 'date-fns';
import { ClassAttendanceCard } from '@/features/attendance/components/ClassAttendanceCard';
import { StudentAttendanceTrend } from '@/features/attendance/components/StudentAttendanceTrend';
import { apiClient } from '@/lib/axios';
import type { ApiResponse } from '@/lib/api-types';
import type { BranchAttendanceStat } from '@/features/attendance/types';
import { useAuthStore } from '@/features/auth/store';

export function AttendanceStatsPage(): JSX.Element {
  const branchId = useAuthStore((s) => s.user?.branches[0]?.id ?? 1);

  const trendData = Array.from({ length: 4 }, (_, i) => {
    const weekStart = subWeeks(new Date(), 3 - i);
    return { week: format(weekStart, 'M/d'), rate: 75 + i * 5 };
  });

  const { data: branchStat } = useQuery({
    queryKey: ['branch-stat', branchId],
    queryFn: async () => {
      const { data } = await apiClient.get<ApiResponse<BranchAttendanceStat>>(
        `/stats/attendance/branch/${branchId}`,
      );
      return data.data;
    },
  });

  return (
    <div className="space-y-6">
      <h1 className="font-serif text-xl font-semibold">出勤统计</h1>
      <StudentAttendanceTrend data={trendData} />
      {branchStat ? (
        <ClassAttendanceCard
          title="校区汇总"
          stat={{
            total: branchStat.total,
            present: branchStat.present,
            absent: branchStat.absent,
            leave: branchStat.leave,
            rate: Number(branchStat.rate),
          }}
        />
      ) : (
        <p className="text-sm text-muted-fg">加载中…</p>
      )}
    </div>
  );
}
