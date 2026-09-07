import { useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DialogFrame } from '@/components/ui/Dialog';
import { Label } from '@/components/ui/Label';
import { SimpleSelect } from '@/components/ui/Select';
import { listClassGroups } from '@/features/course/api';
import { ApiError, formatApiErrorMessage } from '@/lib/api';
import { toast } from '@/lib/toast';
import { studentApi } from '../api';
import type { Student } from '../types';

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  student: Student;
  onDone: () => void;
};

export function TransferClassDialog({
  open,
  onOpenChange,
  student,
  onDone,
}: Props): JSX.Element {
  const currentGroups = student.classGroups ?? [];
  const isAssign = currentGroups.length === 0;
  const [fromClassGroupId, setFromClassGroupId] = useState('');
  const [toClassGroupId, setToClassGroupId] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open) return;
    const first = student.classGroups?.[0];
    setFromClassGroupId(first ? String(first.id) : '');
    setToClassGroupId('');
  }, [open, student.id, student.classGroups]);

  const { data: groupsPage, isLoading } = useQuery({
    queryKey: ['class-groups-for-transfer', student.branchId],
    queryFn: () => listClassGroups(1, 100, student.branchId),
    enabled: open,
  });

  const toOptions = useMemo(() => {
    const items = groupsPage?.items ?? [];
    return items
      .filter((g) => g.status === 1)
      .filter((g) => String(g.id) !== fromClassGroupId)
      .map((g) => ({
        value: String(g.id),
        label: `${g.name}${g.courseName ? ` · ${g.courseName}` : ''}（${g.currentCount}/${g.capacity}）`,
      }));
  }, [groupsPage?.items, fromClassGroupId]);

  const fromOptions = useMemo(
    () => currentGroups.map((g) => ({ value: String(g.id), label: g.name })),
    [currentGroups],
  );

  const canSubmit =
    Boolean(toClassGroupId) && (isAssign || Boolean(fromClassGroupId)) && !submitting;

  const submit = async () => {
    if (!canSubmit) return;
    setSubmitting(true);
    try {
      if (isAssign) {
        await studentApi.bulkAssignClass({
          studentIds: [student.id],
          classGroupId: toClassGroupId,
        });
        toast.success('分班成功');
      } else {
        await studentApi.bulkTransferClass({
          studentIds: [student.id],
          fromClassGroupId,
          toClassGroupId,
        });
        toast.success('调班成功');
      }
      onDone();
      onOpenChange(false);
    } catch (e) {
      toast.error(
        e instanceof ApiError
          ? formatApiErrorMessage(e.payload, e.message)
          : e instanceof Error
            ? e.message
            : '操作失败',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <DialogFrame
      open={open}
      onOpenChange={onOpenChange}
      title={isAssign ? '分班' : '调班'}
      description={
        isAssign
          ? `为「${student.name}」选择班级`
          : `将「${student.name}」调至其他班级（需同一校区）`
      }
      footer={
        <Button onClick={() => void submit()} disabled={!canSubmit}>
          {submitting ? '提交中…' : '确认'}
        </Button>
      }
    >
      <div className="space-y-4">
        {!isAssign ? (
          <div>
            <Label>当前班级</Label>
            {fromOptions.length > 1 ? (
              <SimpleSelect
                className="mt-1"
                aria-label="当前班级"
                options={fromOptions}
                value={fromClassGroupId}
                onValueChange={setFromClassGroupId}
              />
            ) : (
              <p className="mt-1 text-sm text-foreground">
                {fromOptions[0]?.label ?? '—'}
              </p>
            )}
          </div>
        ) : null}
        <div>
          <Label>目标班级</Label>
          {isLoading ? (
            <p className="mt-1 text-sm text-muted-fg">加载班级列表…</p>
          ) : toOptions.length === 0 ? (
            <p className="mt-1 text-sm text-muted-fg">当前校区暂无可用班级</p>
          ) : (
            <SimpleSelect
              className="mt-1"
              aria-label="目标班级"
              options={toOptions}
              value={toClassGroupId}
              onValueChange={setToClassGroupId}
              placeholder="请选择班级"
            />
          )}
        </div>
      </div>
    </DialogFrame>
  );
}
