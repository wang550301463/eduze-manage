import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/ui/EmptyState';
import { fetchLeaves } from '@/features/attendance/api';
import { LeaveApprovalActions } from '@/features/attendance/components/LeaveApprovalActions';
import { LeaveRequestFormDialog } from '@/features/attendance/components/LeaveRequestFormDialog';
import { useHasPermission } from '@/lib/permissions';
import type { EntityId } from '../types';

type Props = {
  studentId: EntityId | number;
  studentName: string;
  enrollNo: string;
};

export function StudentLeaveTab({ studentId, studentName, enrollNo }: Props): JSX.Element {
  const [formOpen, setFormOpen] = useState(false);
  const canRead = useHasPermission('leave:read');
  const canWrite = useHasPermission('leave:write');
  const queryClient = useQueryClient();
  const {
    data = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['leaves', 'student', String(studentId)],
    queryFn: () => fetchLeaves({ studentId }),
    enabled: canRead && studentId !== '',
  });
  const refresh = () => {
    void queryClient.invalidateQueries({ queryKey: ['leaves'] });
  };

  if (studentId === '') return <EmptyState title="请选择学员" />;
  if (!canRead) return <EmptyState title="暂无查看请假记录的权限" />;

  const leaves = data.filter((leave) => String(leave.studentId) === String(studentId));
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm text-muted-fg">该学员的请假申请与审批记录</p>
        {canWrite ? (
          <Button size="sm" onClick={() => setFormOpen(true)}>
            录入请假
          </Button>
        ) : null}
      </div>
      {isLoading ? (
        <p role="status" className="py-8 text-center text-sm text-muted-fg">
          正在加载请假记录…
        </p>
      ) : isError ? (
        <div className="space-y-3 rounded-lg border border-border p-4">
          <p role="alert" className="text-sm text-error">
            {error instanceof Error ? error.message : '无法加载请假记录'}
          </p>
          <Button variant="secondary" size="sm" onClick={() => void refetch()}>
            重试
          </Button>
        </div>
      ) : leaves.length === 0 ? (
        <EmptyState title="暂无请假记录" description="该学员尚未提交请假申请。" />
      ) : (
        <ul className="divide-y divide-border rounded-xl border border-border bg-card/70">
          {leaves.map((leave) => (
            <li key={leave.id} className="space-y-3 p-4 text-sm">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="font-medium">
                  {leave.leaveStartDate} ~ {leave.leaveEndDate}
                </p>
                <span className="rounded-full bg-muted px-2 py-1 text-xs">{leave.statusLabel}</span>
              </div>
              <p className="whitespace-pre-wrap break-words text-muted-fg">
                {leave.reason || '未填写请假原因'}
              </p>
              <LeaveApprovalActions leave={leave} onDone={refresh} compact />
            </li>
          ))}
        </ul>
      )}
      {canWrite ? (
        <LeaveRequestFormDialog
          open={formOpen}
          student={{ id: studentId, name: studentName, enrollNo }}
          onClose={() => setFormOpen(false)}
          onDone={refresh}
        />
      ) : null}
    </div>
  );
}
