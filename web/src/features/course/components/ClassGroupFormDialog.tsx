import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DialogFrame } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { SimpleSelect } from '@/components/ui/Select';
import { createClassGroup, listCourses } from '@/features/course/api';
import { toast } from '@/lib/toast';

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  branchId: number;
  onSaved: () => void;
};

export function ClassGroupFormDialog({ open, onOpenChange, branchId, onSaved }: Props): JSX.Element {
  const [name, setName] = useState('');
  const [courseId, setCourseId] = useState('');
  const [capacity, setCapacity] = useState('10');

  const coursesQuery = useQuery({
    queryKey: ['courses', 'all'],
    queryFn: () => listCourses(1, 100),
    enabled: open,
  });

  const saveMutation = useMutation({
    mutationFn: () =>
      createClassGroup({
        branchId,
        name,
        // 雪花 Long 必须保持字符串，Number() 会丢精度导致「课程不存在」
        courseId: courseId || null,
        capacity: Number(capacity),
      }),
    onSuccess: () => {
      toast.success('班级已创建');
      onOpenChange(false);
      onSaved();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <DialogFrame
      open={open}
      onOpenChange={onOpenChange}
      title="新建班级"
      footer={<Button onClick={() => saveMutation.mutate()}>保存</Button>}
    >
      <div className="space-y-3">
        <div>
          <Label>班级名称</Label>
          <Input value={name} onChange={(e) => setName(e.target.value)} className="mt-1" />
        </div>
        <div>
          <Label>课程</Label>
          <SimpleSelect
            className="mt-1"
            aria-label="课程"
            value={courseId}
            onValueChange={setCourseId}
            options={(coursesQuery.data?.items ?? []).map((c) => ({
              value: String(c.id),
              label: c.name,
            }))}
          />
        </div>
        <div>
          <Label>容量</Label>
          <Input type="number" value={capacity} onChange={(e) => setCapacity(e.target.value)} className="mt-1" />
        </div>
      </div>
    </DialogFrame>
  );
}
