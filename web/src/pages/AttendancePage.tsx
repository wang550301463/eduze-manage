import { EmptyState } from '@/components/ui/EmptyState';

export function AttendancePage(): JSX.Element {
  return (
    <div data-testid="attendance-page" className="space-y-4">
      <h1 className="font-serif text-xl font-semibold">签到工作台</h1>
      <EmptyState title="签到模块" description="完整功能在阶段 H 实现。" />
    </div>
  );
}
