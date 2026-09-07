import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DialogFrame } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { bulkGenerateLessons } from '@/features/lesson/api';
import { toast } from '@/lib/toast';

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  classGroupId: number;
  onDone: () => void;
};

export function BulkGenerateDialog({ open, onOpenChange, classGroupId, onDone }: Props): JSX.Element {
  const [weekdays, setWeekdays] = useState('3,5');
  const [weeks, setWeeks] = useState('8');
  const [fromDate, setFromDate] = useState('2026-05-11');

  const mutation = useMutation({
    mutationFn: () =>
      bulkGenerateLessons({
        classGroupId,
        weekdays: weekdays.split(',').map((s) => Number(s.trim())),
        startTime: '09:00',
        endTime: '10:30',
        weeks: Number(weeks),
        fromDate,
      }),
    onSuccess: (res) => {
      toast.success(`已生成 ${res.generated} 节课`);
      if (res.conflicts.length > 0) {
        toast.info(`跳过 ${res.conflicts.length} 个冲突时段`);
      }
      onOpenChange(false);
      onDone();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <DialogFrame
      open={open}
      onOpenChange={onOpenChange}
      title="按周批量生成课次"
      footer={<Button onClick={() => mutation.mutate()}>生成</Button>}
    >
      <div className="space-y-3">
        <div>
          <Label>起始日期</Label>
          <Input type="date" value={fromDate} onChange={(e) => setFromDate(e.target.value)} className="mt-1" />
        </div>
        <div>
          <Label>星期（1=周一，逗号分隔）</Label>
          <Input value={weekdays} onChange={(e) => setWeekdays(e.target.value)} className="mt-1" />
        </div>
        <div>
          <Label>周数</Label>
          <Input type="number" value={weeks} onChange={(e) => setWeeks(e.target.value)} className="mt-1" />
        </div>
      </div>
    </DialogFrame>
  );
}
