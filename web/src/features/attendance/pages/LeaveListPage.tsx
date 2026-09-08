import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { LeaveApprovalActions } from '@/features/attendance/components/LeaveApprovalActions';
import { LeaveDetailSheet } from '@/features/attendance/components/LeaveDetailSheet';
import { LeaveRequestFormDialog } from '@/features/attendance/components/LeaveRequestFormDialog';
import { fetchLeaves } from '@/features/attendance/api';
import type { LeaveRecord } from '@/features/attendance/types';
import { cn } from '@/lib/cn';
import { useHasPermission } from '@/lib/permissions';

const tabs = [
  { status: 1, label: '待审批' },
  { status: 2, label: '已批准' },
  { status: 3, label: '已拒绝' },
] as const;

export function LeaveListPage(): JSX.Element {
  const [tab, setTab] = useState(1);
  const [selected, setSelected] = useState<LeaveRecord | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const queryClient = useQueryClient();
  const canWrite = useHasPermission('leave:write');

  const {
    data: leaves = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['leaves', tab],
    queryFn: () => fetchLeaves({ status: tab }),
  });

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['leaves'] });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">请假管理</h1>
        {canWrite ? <Button onClick={() => setFormOpen(true)}>录入请假</Button> : null}
      </div>

      <div className="inline-flex rounded-lg bg-muted p-1">
        {tabs.map((t) => (
          <button
            key={t.status}
            type="button"
            className={cn(
              'rounded-md px-4 py-1.5 text-sm font-medium transition-colors',
              tab === t.status
                ? 'bg-white text-foreground shadow-sm'
                : 'text-muted-fg hover:text-foreground',
            )}
            onClick={() => setTab(t.status)}
          >
            {t.label}
          </button>
        ))}
      </div>

      <ul className="divide-y divide-border overflow-hidden rounded-xl border border-border bg-white">
        {isLoading ? (
          <li className="p-4 text-sm text-muted-fg">加载中…</li>
        ) : isError ? (
          <li className="space-y-3 p-4">
            <p role="alert" className="text-sm text-error">
              {error instanceof Error ? error.message : '无法加载请假记录'}
            </p>
            <Button variant="secondary" size="sm" onClick={() => void refetch()}>
              重试
            </Button>
          </li>
        ) : leaves.length === 0 ? (
          <li className="p-4 text-sm text-muted-fg">暂无记录</li>
        ) : (
          leaves.map((leave) => (
            <li
              key={leave.id}
              className="flex items-center justify-between gap-4 pr-4 hover:bg-muted/50"
            >
              <button
                type="button"
                className="min-w-0 flex-1 p-4 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary/40"
                onClick={() => setSelected(leave)}
              >
                <span className="block font-medium">
                  {leave.studentName} · {leave.leaveStartDate} ~ {leave.leaveEndDate}
                </span>
                <span className="block break-words text-sm text-muted-fg">
                  {leave.reason ?? '无原因'}
                </span>
              </button>
              <LeaveApprovalActions leave={leave} onDone={refresh} compact />
            </li>
          ))
        )}
      </ul>

      <LeaveDetailSheet
        leave={selected}
        onClose={() => setSelected(null)}
        onDone={() => {
          setSelected(null);
          void refresh();
        }}
      />
      {canWrite ? (
        <LeaveRequestFormDialog
          open={formOpen}
          onClose={() => setFormOpen(false)}
          onDone={refresh}
        />
      ) : null}
    </div>
  );
}
