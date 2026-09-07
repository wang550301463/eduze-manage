import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { BranchAttendanceKpis } from '@/features/attendance/components/BranchAttendanceKpis';
import { fetchDashboardKpis } from '@/features/attendance/api';
import { useAuthStore } from '@/features/auth/store';

export function PrincipalDashboardPage(): JSX.Element {
  const user = useAuthStore((s) => s.user);
  const branchId = user?.branches[0]?.id ?? 1;

  const { data, isLoading } = useQuery({
    queryKey: ['dashboard-kpis', branchId],
    queryFn: () => fetchDashboardKpis(branchId),
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-foreground">
            {user?.name ? `你好，${user.name}` : '校长看板'}
          </h1>
          <p className="mt-1 text-sm text-muted-fg">校区运营概览</p>
        </div>
        <Link to="/attendance">
          <Button variant="secondary">前往签到工作台</Button>
        </Link>
      </div>
      {isLoading || !data ? (
        <div className="rounded-xl border border-border bg-white p-6">
          <p className="text-sm text-muted-fg">加载 KPI…</p>
        </div>
      ) : (
        <div className="rounded-xl border border-border bg-white p-4">
          <BranchAttendanceKpis data={data} />
        </div>
      )}
    </div>
  );
}
