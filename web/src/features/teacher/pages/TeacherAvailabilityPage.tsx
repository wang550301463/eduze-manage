import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Button } from '@/components/ui/Button';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { SimpleSelect } from '@/components/ui/Select';
import { listClassGroups } from '@/features/course/api';
import { ApiError, formatApiErrorMessage } from '@/lib/api';
import { toast } from '@/lib/toast';
import { branchApi } from '@/features/student/api';
import { teacherApi } from '../api';
import type { EntityId, TeacherAvailability } from '../types';

const DOW_LABELS = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'];

function minutesToHHMM(min: number): string {
  const h = Math.floor(min / 60);
  const m = min % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

function hhmmToMinutes(s: string): number {
  const [h, m] = s.split(':').map((p) => Number(p));
  return h * 60 + m;
}

export function TeacherAvailabilityPage(): JSX.Element {
  const qc = useQueryClient();
  const [selectedTeacherId, setSelectedTeacherId] = useState<EntityId | undefined>(undefined);
  const [selectedBranchId, setSelectedBranchId] = useState<EntityId | undefined>(undefined);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<TeacherAvailability | null>(null);
  const [form, setForm] = useState({
    dayOfWeek: '6',
    startTime: '09:00',
    endTime: '10:30',
    capacity: '8',
    validFrom: new Date().toISOString().slice(0, 10),
    validTo: '',
    status: '1',
    note: '',
  });

  const branchesQuery = useQuery({ queryKey: ['branches'], queryFn: () => branchApi.list() });
  const teachersQuery = useQuery({
    queryKey: ['teachers', selectedBranchId],
    queryFn: () => teacherApi.list(selectedBranchId),
  });
  const availQuery = useQuery({
    queryKey: ['teacher-availabilities', selectedTeacherId],
    queryFn: () => teacherApi.listAvailabilities(selectedTeacherId!),
    enabled: !!selectedTeacherId,
  });
  const groupsQuery = useQuery({
    queryKey: ['class-groups', 'for-availability', selectedBranchId],
    queryFn: () => listClassGroups(1, 200, selectedBranchId),
    enabled: !!selectedBranchId,
  });

  const groupNameById = useMemo(() => {
    const map = new Map<string, string>();
    for (const g of groupsQuery.data?.items ?? []) {
      map.set(String(g.id), g.name);
    }
    return map;
  }, [groupsQuery.data?.items]);

  const boundLabel = (a: TeacherAvailability): string => {
    if (!a.boundClassGroupId) return '未绑定';
    if (a.boundClassGroupName) return a.boundClassGroupName;
    return groupNameById.get(String(a.boundClassGroupId)) ?? '已绑定';
  };

  const saveMut = useMutation({
    mutationFn: async () => {
      const branchId =
        selectedBranchId ?? teachersQuery.data?.find((t) => t.id === selectedTeacherId)?.branchId;
      if (!branchId) throw new Error('请选择校区');
      const body = {
        branchId,
        dayOfWeek: Number(form.dayOfWeek),
        startMinute: hhmmToMinutes(form.startTime),
        endMinute: hhmmToMinutes(form.endTime),
        capacity: Number(form.capacity),
        validFrom: form.validFrom,
        validTo: form.validTo || null,
        status: Number(form.status),
        note: form.note || undefined,
      };
      if (editing) {
        return teacherApi.updateAvailability(editing.id, body);
      }
      if (!selectedTeacherId) throw new Error('请选择老师');
      return teacherApi.createAvailability(selectedTeacherId, body);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['teacher-availabilities', selectedTeacherId] });
      setDialogOpen(false);
      setEditing(null);
      toast.success('已保存');
    },
    onError: (e: Error) => {
      toast.error(
        e instanceof ApiError ? formatApiErrorMessage(e.payload, e.message) : e.message,
      );
    },
  });

  const deleteMut = useMutation({
    mutationFn: (id: EntityId) => teacherApi.deleteAvailability(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['teacher-availabilities', selectedTeacherId] });
      toast.success('已删除');
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const openCreate = () => {
    setEditing(null);
    setForm({
      dayOfWeek: '6',
      startTime: '09:00',
      endTime: '10:30',
      capacity: '8',
      validFrom: new Date().toISOString().slice(0, 10),
      validTo: '',
      status: '1',
      note: '',
    });
    setDialogOpen(true);
  };

  const openEdit = (a: TeacherAvailability) => {
    setEditing(a);
    setForm({
      dayOfWeek: String(a.dayOfWeek),
      startTime: minutesToHHMM(a.startMinute),
      endTime: minutesToHHMM(a.endMinute),
      capacity: String(a.capacity),
      validFrom: a.validFrom,
      validTo: a.validTo ?? '',
      status: String(a.status),
      note: a.note ?? '',
    });
    setDialogOpen(true);
  };

  return (
    <div className="space-y-4 pb-24" data-testid="teacher-availability-page">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-2xl font-semibold tracking-tight">老师可用时段</h1>
        <div className="flex flex-wrap gap-2">
          <SimpleSelect
            value={selectedBranchId}
            onValueChange={(v) => {
              setSelectedBranchId(v);
              setSelectedTeacherId(undefined);
            }}
            options={(branchesQuery.data ?? []).map((b) => ({ value: String(b.id), label: b.name }))}
            placeholder="校区"
          />
          <SimpleSelect
            value={selectedTeacherId}
            onValueChange={setSelectedTeacherId}
            options={(teachersQuery.data ?? []).map((t) => ({
              value: String(t.id),
              label: t.name || t.username,
            }))}
            placeholder="选择老师"
          />
          <Button onClick={openCreate} disabled={!selectedTeacherId}>
            新增时段
          </Button>
        </div>
      </div>

      <div className="overflow-hidden rounded-xl border border-border bg-white">
        <table className="w-full text-sm">
          <thead className="bg-muted text-muted-fg">
            <tr>
              <th className="px-3 py-2 text-left">星期</th>
              <th className="px-3 py-2 text-left">时段</th>
              <th className="px-3 py-2 text-left">容量</th>
              <th className="px-3 py-2 text-left">分组</th>
              <th className="px-3 py-2 text-left">生效起止</th>
              <th className="px-3 py-2 text-left">状态</th>
              <th className="px-3 py-2 text-left">备注</th>
              <th className="px-3 py-2 text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            {!selectedTeacherId ? (
              <tr>
                <td colSpan={8} className="px-3 py-6 text-center text-muted-fg">
                  请先选择老师
                </td>
              </tr>
            ) : (availQuery.data ?? []).length === 0 ? (
              <tr>
                <td colSpan={8} className="px-3 py-6 text-center text-muted-fg">
                  该老师暂无可用时段，点击右上「新增时段」开始配置
                </td>
              </tr>
            ) : (
              (availQuery.data ?? []).map((a) => {
                const bound = Boolean(a.boundClassGroupId);
                return (
                  <tr key={String(a.id)} className="border-t border-border">
                    <td className="px-3 py-2">{DOW_LABELS[a.dayOfWeek]}</td>
                    <td className="px-3 py-2">
                      {minutesToHHMM(a.startMinute)} - {minutesToHHMM(a.endMinute)}
                    </td>
                    <td className="px-3 py-2">{a.capacity}</td>
                    <td className="px-3 py-2">
                      {bound ? (
                        <span className="text-foreground">{boundLabel(a)}</span>
                      ) : (
                        <span className="text-muted-fg">未绑定</span>
                      )}
                    </td>
                    <td className="px-3 py-2">
                      {a.validFrom}
                      {a.validTo ? ` ~ ${a.validTo}` : ''}
                    </td>
                    <td className="px-3 py-2">{a.status === 1 ? '启用' : '停用'}</td>
                    <td className="px-3 py-2">{a.note ?? ''}</td>
                    <td className="px-3 py-2 text-right">
                      <Button size="sm" variant="ghost" onClick={() => openEdit(a)}>
                        编辑
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        disabled={bound}
                        title={
                          bound
                            ? '该时段已绑定分组，请先换绑或解散分组后再删除'
                            : undefined
                        }
                        onClick={() => {
                          if (bound) {
                            toast.info('该时段已绑定分组，请先换绑或解散分组后再删除');
                            return;
                          }
                          if (confirm('确认删除？')) deleteMut.mutate(a.id);
                        }}
                      >
                        删除
                      </Button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{editing ? '编辑可用时段' : '新增可用时段'}</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <div>
              <Label>星期</Label>
              <SimpleSelect
                value={form.dayOfWeek}
                onValueChange={(v) => setForm({ ...form, dayOfWeek: v })}
                options={[
                  { value: '1', label: '周一' },
                  { value: '2', label: '周二' },
                  { value: '3', label: '周三' },
                  { value: '4', label: '周四' },
                  { value: '5', label: '周五' },
                  { value: '6', label: '周六' },
                  { value: '7', label: '周日' },
                ]}
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label>开始时间</Label>
                <Input
                  className="mt-1"
                  type="time"
                  value={form.startTime}
                  onChange={(e) => setForm({ ...form, startTime: e.target.value })}
                />
              </div>
              <div>
                <Label>结束时间</Label>
                <Input
                  className="mt-1"
                  type="time"
                  value={form.endTime}
                  onChange={(e) => setForm({ ...form, endTime: e.target.value })}
                />
              </div>
            </div>
            <div>
              <Label>容量</Label>
              <Input
                className="mt-1"
                type="number"
                min="1"
                value={form.capacity}
                onChange={(e) => setForm({ ...form, capacity: e.target.value })}
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label>生效起始</Label>
                <Input
                  className="mt-1"
                  type="date"
                  value={form.validFrom}
                  onChange={(e) => setForm({ ...form, validFrom: e.target.value })}
                />
              </div>
              <div>
                <Label>生效截止</Label>
                <Input
                  className="mt-1"
                  type="date"
                  value={form.validTo}
                  onChange={(e) => setForm({ ...form, validTo: e.target.value })}
                />
              </div>
            </div>
            <div>
              <Label>状态</Label>
              <SimpleSelect
                value={form.status}
                onValueChange={(v) => setForm({ ...form, status: v })}
                options={[
                  { value: '1', label: '启用' },
                  { value: '0', label: '停用' },
                ]}
              />
            </div>
            <div>
              <Label>备注</Label>
              <Input
                className="mt-1"
                value={form.note}
                onChange={(e) => setForm({ ...form, note: e.target.value })}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setDialogOpen(false)}>
              取消
            </Button>
            <Button onClick={() => saveMut.mutate()} disabled={saveMut.isPending}>
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
