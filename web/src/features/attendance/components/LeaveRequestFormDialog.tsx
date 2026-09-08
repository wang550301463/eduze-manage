import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DialogFrame } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Textarea } from '@/components/ui/Textarea';
import { createLeave } from '@/features/attendance/api';
import { useAuthStore } from '@/features/auth/store';
import { studentApi } from '@/features/student/api';
import type { EntityId } from '@/features/student/types';
import { toast } from '@/lib/toast';

type Props = {
  open: boolean;
  onClose: () => void;
  onDone: () => void;
  student?: { id: EntityId | number; name: string; enrollNo: string };
};

export function LeaveRequestFormDialog(props: Props): JSX.Element {
  // 每次打开重新挂载表单，避免沿用上次学员、日期及错误信息。
  return props.open ? <LeaveRequestForm key={props.student?.id ?? 'new'} {...props} /> : <></>;
}

function LeaveRequestForm({ open, onClose, onDone, student }: Props): JSX.Element {
  const userId = useAuthStore((s) => s.user?.id);
  const branchIds = useAuthStore((s) => s.branchIds);
  const [selectedStudent, setSelectedStudent] = useState(student);
  const [keyword, setKeyword] = useState('');
  const [search, setSearch] = useState('');
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const timer = window.setTimeout(() => setSearch(keyword.trim()), 250);
    return () => window.clearTimeout(timer);
  }, [keyword]);

  const students = useQuery({
    queryKey: ['leave-student-search', userId, branchIds, search],
    queryFn: () => studentApi.list({ keyword: search || undefined, page: '1', size: '20' }),
    enabled: !selectedStudent,
  });

  const submit = async () => {
    if (loading) return;
    if (!selectedStudent) {
      setError('请选择学员');
      return;
    }
    if (!start || !end) {
      setError('请填写开始日期与结束日期');
      return;
    }
    if (end < start) {
      setError('结束日期不能早于开始日期');
      return;
    }
    setError('');
    setLoading(true);
    try {
      await createLeave({
        // 雪花 ID 保持字符串，避免 Number() 精度丢失
        studentId: selectedStudent.id,
        leaveStartDate: start,
        leaveEndDate: end,
        reason: reason.trim() || undefined,
      });
      toast.success('请假已提交');
      onDone();
      onClose();
    } catch (e) {
      setError(e instanceof Error ? e.message : '提交失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <DialogFrame
      open={open}
      onOpenChange={(v) => !v && !loading && onClose()}
      title="录入请假"
      description="选择学员和请假日期，提交后由有权限的老师审批。"
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={loading}>
            取消
          </Button>
          <Button onClick={submit} disabled={loading}>
            {loading ? '提交中…' : '提交'}
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        <div>
          {selectedStudent ? (
            <div className="flex items-center justify-between gap-3 rounded-lg border border-border bg-muted/50 p-3">
              <div>
                <p className="text-xs text-muted-fg">请假学员</p>
                <p className="text-sm font-medium">
                  {selectedStudent.name} · {selectedStudent.enrollNo}
                </p>
              </div>
              {!student ? (
                <Button
                  size="sm"
                  variant="ghost"
                  disabled={loading}
                  onClick={() => setSelectedStudent(undefined)}
                >
                  更换学员
                </Button>
              ) : null}
            </div>
          ) : (
            <>
              <Label htmlFor="leave-student-search">搜索学员</Label>
              <Input
                id="leave-student-search"
                placeholder="输入姓名或入园编号"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                className="mt-1"
                autoComplete="off"
              />
              <div className="mt-2 max-h-48 overflow-y-auto rounded-lg border border-border">
                {students.isLoading || search !== keyword.trim() ? (
                  <p role="status" className="p-3 text-sm text-muted-fg">
                    正在搜索学员…
                  </p>
                ) : students.isError ? (
                  <div className="space-y-2 p-3">
                    <p role="alert" className="text-sm text-error">
                      {students.error instanceof Error ? students.error.message : '学员加载失败'}
                    </p>
                    <Button size="sm" variant="secondary" onClick={() => void students.refetch()}>
                      重试搜索
                    </Button>
                  </div>
                ) : students.data?.records.length ? (
                  <ul className="divide-y divide-border">
                    {students.data.records.map((candidate) => (
                      <li key={candidate.id}>
                        <button
                          type="button"
                          className="flex w-full items-center justify-between gap-3 px-3 py-2.5 text-left text-sm hover:bg-muted focus-visible:bg-muted focus-visible:outline-none"
                          onClick={() => {
                            setSelectedStudent(candidate);
                            setError('');
                          }}
                        >
                          <span className="font-medium">{candidate.name}</span>
                          <span className="text-muted-fg">{candidate.enrollNo}</span>
                        </button>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="p-3 text-sm text-muted-fg">未找到学员，请尝试其他姓名或入园编号</p>
                )}
              </div>
              {students.data && students.data.total > students.data.records.length ? (
                <p className="mt-1 text-xs text-muted-fg">
                  当前显示前 20 位学员，请输入更完整的姓名或入园编号。
                </p>
              ) : null}
            </>
          )}
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <Label htmlFor="leave-start">开始日期</Label>
            <Input
              id="leave-start"
              type="date"
              value={start}
              disabled={loading}
              onChange={(e) => setStart(e.target.value)}
              className="mt-1"
            />
          </div>
          <div>
            <Label htmlFor="leave-end">结束日期</Label>
            <Input
              id="leave-end"
              type="date"
              value={end}
              min={start || undefined}
              disabled={loading}
              onChange={(e) => setEnd(e.target.value)}
              className="mt-1"
            />
          </div>
        </div>
        <div>
          <Label htmlFor="leave-reason">原因</Label>
          <Textarea
            id="leave-reason"
            value={reason}
            disabled={loading}
            onChange={(e) => setReason(e.target.value)}
            className="mt-1"
            rows={3}
          />
        </div>
        {error ? (
          <p role="alert" className="text-sm text-error">
            {error}
          </p>
        ) : null}
      </div>
    </DialogFrame>
  );
}
