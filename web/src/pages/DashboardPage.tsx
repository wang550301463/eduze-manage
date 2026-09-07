import { KPICard } from '@/components/ui/KPICard';
import { useAuthStore } from '@/features/auth/store';

export function DashboardPage(): JSX.Element {
  const user = useAuthStore((s) => s.user);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-serif text-2xl font-bold text-foreground">
          {user?.name ? `你好，${user.name}` : '工作台'}
        </h1>
        <p className="mt-1 text-sm text-muted-fg">欢迎使用 EduZE Manage 机构后台</p>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KPICard title="在读学员" value={0} />
        <KPICard title="本周课次" value={0} />
        <KPICard title="待审批请假" value={0} />
        <KPICard title="今日签到" value={0} />
      </div>
    </div>
  );
}
