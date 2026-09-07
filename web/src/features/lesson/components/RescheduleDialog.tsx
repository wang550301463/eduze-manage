import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DialogFrame } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { checkConflict, rescheduleLesson } from '@/features/lesson/api';
import { ConflictWarningDialog } from '@/features/lesson/components/ConflictWarningDialog';
import type { ConflictReport, Lesson } from '@/features/lesson/types';
import { toast } from '@/lib/toast';

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  lesson: Lesson;
  onDone: () => void;
};

export function RescheduleDialog({ open, onOpenChange, lesson, onDone }: Props): JSX.Element {
  const [startAt, setStartAt] = useState(lesson.startAt.slice(0, 16));
  const [endAt, setEndAt] = useState(lesson.endAt.slice(0, 16));
  const [reason, setReason] = useState('');
  const [warnOpen, setWarnOpen] = useState(false);
  const [report, setReport] = useState<ConflictReport | null>(null);

  const submit = async (force = false) => {
    const body = {
      lessonId: lesson.id,
      branchId: lesson.branchId,
      classGroupId: lesson.classGroupId,
      classRoomId: lesson.classRoomId,
      teacherId: lesson.teacherId,
      startAt: `${startAt}:00`,
      endAt: `${endAt}:00`,
    };
    if (!force) {
      const conflict = await checkConflict(body);
      if (conflict.hasConflict) {
        setReport(conflict);
        setWarnOpen(true);
        return;
      }
    }
    await rescheduleLesson(lesson.id, {
      startAt: body.startAt,
      endAt: body.endAt,
      teacherId: body.teacherId,
      classRoomId: body.classRoomId,
      reason,
    });
    toast.success('调课成功');
    onOpenChange(false);
    onDone();
  };

  return (
    <>
      <DialogFrame
        open={open}
        onOpenChange={onOpenChange}
        title="调课"
        footer={<Button onClick={() => void submit()}>保存</Button>}
      >
        <div className="space-y-3">
          <div>
            <Label>开始</Label>
            <Input type="datetime-local" value={startAt} onChange={(e) => setStartAt(e.target.value)} className="mt-1" />
          </div>
          <div>
            <Label>结束</Label>
            <Input type="datetime-local" value={endAt} onChange={(e) => setEndAt(e.target.value)} className="mt-1" />
          </div>
          <div>
            <Label>原因</Label>
            <Input value={reason} onChange={(e) => setReason(e.target.value)} className="mt-1" />
          </div>
        </div>
      </DialogFrame>
      <ConflictWarningDialog
        open={warnOpen}
        onOpenChange={setWarnOpen}
        report={report}
        onConfirm={() => {
          setWarnOpen(false);
          void submit(true);
        }}
      />
    </>
  );
}
