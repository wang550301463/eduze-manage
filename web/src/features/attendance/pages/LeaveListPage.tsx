import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { LeaveApprovalActions } from '@/features/attendance/components/LeaveApprovalActions';
import { LeaveDetailSheet } from '@/features/attendance/components/LeaveDetailSheet';
import { LeaveRequestFormDialog } from '@/features/attendance/components/LeaveRequestFormDialog';
import { fetchLeaves } from '@/features/attendance/api';
import type { LeaveRecord } from '@/features/attendance/types';
import { cn } from '@/lib/cn';

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

  const { data: leaves = [], isLoading } = useQuery({
    queryKey: ['leaves', tab],
    queryFn: () => fetchLeaves({ status: tab }),
  });

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['leaves'] });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="font-serif text-xl font-semibold">请假管理</h1>
        <Button onClick={() => setFormOpen(true)}>录入请假</Button>
      </div>

      <div className="flex gap-2 border-b border-border">
        {tabs.map((t) => (
          <button
            key={t.status}
            type="button"
            className={cn(
              'px-4 py-2 text-sm font-medium',
              tab === t.status ? 'border-b-2 border-primary text-primary' : 'text-muted-fg',
            )}
            onClick={() => setTab(t.status)}
          >
            {t.label}
          </button>
        ))}
      </div>

      <ul className="divide-y divide-border rounded-lg border border-border">
        {isLoading ? (
          <li className="p-4 text-sm text-muted-fg">加载中…</li>
        ) : leaves.length === 0 ? (
          <li className="p-4 text-sm text-muted-fg">暂无记录</li>
        ) : (
          leaves.map((leave) => (
            <li
              key={leave.id}
              className="flex cursor-pointer items-center justify-between gap-4 p-4 hover:bg-muted/50"
              onClick={() => setSelected(leave)}
            >
              <div>
                <p className="font-medium">
                  {leave.studentName} · {leave.leaveStartDate} ~ {leave.leaveEndDate}
                </p>
                <p className="text-sm text-muted-fg">{leave.reason ?? '无原因'}</p>
              </div>
              <LeaveApprovalActions leave={leave} onDone={refresh} compact />
            </li>
          ))
        )}
      </ul>

      <LeaveDetailSheet leave={selected} onClose={() => setSelected(null)} onDone={refresh} />
      <LeaveRequestFormDialog open={formOpen} onClose={() => setFormOpen(false)} onDone={refresh} />
    </div>
  );
}
