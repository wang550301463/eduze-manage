import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DialogFrame } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { SimpleSelect } from '@/components/ui/Select';
import { createClassGroup, listCourses, listUnboundAvailabilities } from '@/features/course/api';
import {
  DAY_OF_WEEK_OPTIONS,
  formatScheduleSlot,
  hhmmToMinutes,
} from '@/features/course/scheduleFormat';
import { teacherApi } from '@/features/teacher/api';
import { toast } from '@/lib/toast';

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  branchId: number | string;
  onSaved: () => void;
};

type BindMode = 'existing' | 'nested';

function todayISO(): string {
  return new Date().toISOString().slice(0, 10);
}

export function ClassGroupFormDialog({ open, onOpenChange, branchId, onSaved }: Props): JSX.Element {
  const [name, setName] = useState('');
  const [courseId, setCourseId] = useState('');
  const [bindMode, setBindMode] = useState<BindMode>('existing');
  const [availabilityId, setAvailabilityId] = useState('');
  const [teacherId, setTeacherId] = useState('');
  const [dayOfWeek, setDayOfWeek] = useState('6');
  const [startTime, setStartTime] = useState('09:00');
  const [endTime, setEndTime] = useState('10:30');
  const [capacity, setCapacity] = useState('8');

  useEffect(() => {
    if (!open) return;
    setName('');
    setCourseId('');
    setBindMode('existing');
    setAvailabilityId('');
    setTeacherId('');
    setDayOfWeek('6');
    setStartTime('09:00');
    setEndTime('10:30');
    setCapacity('8');
  }, [open]);

  const coursesQuery = useQuery({
    queryKey: ['courses', 'all'],
    queryFn: () => listCourses(1, 100),
    enabled: open,
  });

  const unboundQuery = useQuery({
    queryKey: ['unbound-availabilities', branchId],
    queryFn: () => listUnboundAvailabilities(branchId),
    enabled: open && bindMode === 'existing',
  });

  const teachersQuery = useQuery({
    queryKey: ['teachers', branchId],
    queryFn: () => teacherApi.list(branchId),
    enabled: open && bindMode === 'nested',
  });

  const selectedAvail = useMemo(
    () => (unboundQuery.data ?? []).find((a) => String(a.id) === availabilityId),
    [unboundQuery.data, availabilityId],
  );

  const displayCapacity =
    bindMode === 'existing' ? (selectedAvail ? String(selectedAvail.capacity) : '—') : capacity;

  const unboundOptions = useMemo(
    () =>
      (unboundQuery.data ?? []).map((a) => ({
        value: String(a.id),
        label: `${a.teacherName ?? '老师'} · ${formatScheduleSlot(a.dayOfWeek, a.startMinute, a.endMinute)}（容量 ${a.capacity}）`,
      })),
    [unboundQuery.data],
  );

  const canSubmit =
    Boolean(name.trim()) &&
    (bindMode === 'existing'
      ? Boolean(availabilityId)
      : Boolean(teacherId) && Boolean(startTime) && Boolean(endTime) && Number(capacity) > 0);

  const saveMutation = useMutation({
    mutationFn: () => {
      const base = {
        branchId,
        name: name.trim(),
        courseId: courseId || null,
      };
      if (bindMode === 'existing') {
        return createClassGroup({
          ...base,
          teacherAvailabilityId: availabilityId,
        });
      }
      return createClassGroup({
        ...base,
        nestedAvailability: {
          teacherId,
          branchId,
          dayOfWeek: Number(dayOfWeek),
          startMinute: hhmmToMinutes(startTime),
          endMinute: hhmmToMinutes(endTime),
          capacity: Number(capacity),
          validFrom: todayISO(),
          status: 1,
        },
      });
    },
    onSuccess: () => {
      toast.success('分组已创建');
      onOpenChange(false);
      onSaved();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <DialogFrame
      open={open}
      onOpenChange={onOpenChange}
      title="新建分组"
      footer={
        <Button onClick={() => saveMutation.mutate()} disabled={!canSubmit || saveMutation.isPending}>
          {saveMutation.isPending ? '保存中…' : '保存'}
        </Button>
      }
    >
      <div className="space-y-4">
        <div>
          <Label>分组名称</Label>
          <Input value={name} onChange={(e) => setName(e.target.value)} className="mt-1" />
        </div>
        <div>
          <Label>课程（可选）</Label>
          <SimpleSelect
            className="mt-1"
            aria-label="课程"
            value={courseId}
            onValueChange={setCourseId}
            options={(coursesQuery.data?.items ?? []).map((c) => ({
              value: String(c.id),
              label: c.name,
            }))}
            placeholder="不选课程"
          />
        </div>

        <div>
          <Label>绑定可用时段</Label>
          <div className="mt-1 flex gap-2">
            <Button
              type="button"
              size="sm"
              variant={bindMode === 'existing' ? 'default' : 'secondary'}
              onClick={() => setBindMode('existing')}
            >
              选择已有时段
            </Button>
            <Button
              type="button"
              size="sm"
              variant={bindMode === 'nested' ? 'default' : 'secondary'}
              onClick={() => setBindMode('nested')}
            >
              现场新建时段
            </Button>
          </div>
        </div>

        {bindMode === 'existing' ? (
          <div>
            <Label>未绑定时段</Label>
            {unboundQuery.isLoading ? (
              <p className="mt-1 text-sm text-muted-fg">加载中…</p>
            ) : unboundOptions.length === 0 ? (
              <p className="mt-1 text-sm text-muted-fg">
                本校区暂无未绑定的可用时段，可切换「现场新建时段」
              </p>
            ) : (
              <SimpleSelect
                className="mt-1"
                aria-label="未绑定时段"
                value={availabilityId}
                onValueChange={setAvailabilityId}
                options={unboundOptions}
                placeholder="请选择时段"
              />
            )}
          </div>
        ) : (
          <>
            <div>
              <Label>老师</Label>
              <SimpleSelect
                className="mt-1"
                aria-label="老师"
                value={teacherId}
                onValueChange={setTeacherId}
                options={(teachersQuery.data ?? []).map((t) => ({
                  value: String(t.id),
                  label: t.name || t.username,
                }))}
                placeholder="请选择老师"
              />
            </div>
            <div>
              <Label>星期</Label>
              <SimpleSelect
                className="mt-1"
                aria-label="星期"
                value={dayOfWeek}
                onValueChange={setDayOfWeek}
                options={DAY_OF_WEEK_OPTIONS}
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label>开始时间</Label>
                <Input
                  className="mt-1"
                  type="time"
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                />
              </div>
              <div>
                <Label>结束时间</Label>
                <Input
                  className="mt-1"
                  type="time"
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                />
              </div>
            </div>
            <div>
              <Label>容量</Label>
              <Input
                className="mt-1"
                type="number"
                min={1}
                value={capacity}
                onChange={(e) => setCapacity(e.target.value)}
              />
              <p className="mt-1 text-xs text-muted-fg">
                现场新建时段时填写；绑定后分组容量与此时段一致
              </p>
            </div>
          </>
        )}

        {bindMode === 'existing' ? (
          <div>
            <Label>容量（只读，来自时段）</Label>
            <Input className="mt-1" value={displayCapacity} readOnly disabled />
          </div>
        ) : null}
      </div>
    </DialogFrame>
  );
}
