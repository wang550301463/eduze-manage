import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DialogFrame } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Textarea } from '@/components/ui/Textarea';
import { createLeave } from '@/features/attendance/api';
import { toast } from '@/lib/toast';

type Props = {
  open: boolean;
  onClose: () => void;
  onDone: () => void;
};

export function LeaveRequestFormDialog({ open, onClose, onDone }: Props): JSX.Element {
  const [studentId, setStudentId] = useState('');
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async () => {
    if (!studentId || !start || !end) {
      toast.error('请填写学员 ID 与日期');
      return;
    }
    setLoading(true);
    try {
      await createLeave({
        studentId: Number(studentId),
        leaveStartDate: start,
        leaveEndDate: end,
        reason: reason || undefined,
      });
      toast.success('请假已提交');
      onDone();
      onClose();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '提交失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <DialogFrame
      open={open}
      onOpenChange={(v) => !v && onClose()}
      title="录入请假"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            取消
          </Button>
          <Button onClick={submit} disabled={loading}>
            提交
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        <div>
          <Label htmlFor="leave-student">学员 ID</Label>
          <Input
            id="leave-student"
            value={studentId}
            onChange={(e) => setStudentId(e.target.value)}
            className="mt-1"
          />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <Label htmlFor="leave-start">开始日期</Label>
            <Input
              id="leave-start"
              type="date"
              value={start}
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
            onChange={(e) => setReason(e.target.value)}
            className="mt-1"
            rows={3}
          />
        </div>
      </div>
    </DialogFrame>
  );
}
