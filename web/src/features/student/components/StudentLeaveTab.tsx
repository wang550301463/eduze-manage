import { EmptyState } from '@/components/ui/EmptyState';

/** TODO: H10 — 接入请假 API 后替换空态 */
export function StudentLeaveTab(): JSX.Element {
  return (
    <EmptyState title="暂无请假记录" description="第 H 阶段后启用请假 Tab 数据展示" />
  );
}
