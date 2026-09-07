import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DialogFrame } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { cancelLesson } from '@/features/lesson/api';
import { toast } from '@/lib/toast';

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  lessonId: number;
  onDone: () => void;
};

export function CancelLessonDialog({ open, onOpenChange, lessonId, onDone }: Props): JSX.Element {
  const [reason, setReason] = useState('');

  return (
    <DialogFrame
      open={open}
      onOpenChange={onOpenChange}
      title="取消课次"
      footer={
        <Button
          variant="danger"
          onClick={async () => {
            await cancelLesson(lessonId, reason);
            toast.success('课次已取消');
            onOpenChange(false);
            onDone();
          }}
        >
          确认取消
        </Button>
      }
    >
      <div>
        <Label>原因（必填）</Label>
        <Input value={reason} onChange={(e) => setReason(e.target.value)} className="mt-1" />
      </div>
    </DialogFrame>
  );
}
