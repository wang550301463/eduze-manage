import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { SimpleSelect } from '@/components/ui/Select';
import { ApiError, formatApiErrorMessage } from '@/lib/api';
import { toast } from '@/lib/toast';
import { useHasPermission } from '@/lib/permissions';
import { branchApi } from '@/features/student/api';
import { fetchLesson } from '@/features/lesson/api';
import { LessonDetailSheet } from '@/features/lesson/components/LessonDetailSheet';
import { scheduleApi, teacherApi } from '../api';

const DOW_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];

const SPECIAL_SOURCE_OPTIONS = [
  { value: '3', label: '补课' },
  { value: '5', label: '考级' },
  { value: '6', label: '比赛' },
];

function fmtTime(min: number): string {
  const h = Math.floor(min / 60);
  const m = min % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

function toLocalISODate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function mondayISO(date = toLocalISODate(new Date())): string {
  const d = new Date(`${date.slice(0, 10)}T12:00:00`);
  const dow = d.getDay() === 0 ? 7 : d.getDay();
  d.setDate(d.getDate() - (dow - 1));
  return toLocalISODate(d);
}

function addDays(iso: string, days: number): string {
  const d = new Date(`${iso}T12:00:00`);
  d.setDate(d.getDate() + days);
  return toLocalISODate(d);
}

export function WeeklySchedulePage(): JSX.Element {
  const qc = useQueryClient();
  const canWriteLessons = useHasPermission('lesson:write');
  const [searchParams, setSearchParams] = useSearchParams();
  const lessonId = searchParams.get('openId') || searchParams.get('lessonId');
  const [weekStart, setWeekStart] = useState(mondayISO());
  const [branchId, setBranchId] = useState<string | undefined>(undefined);
  const [bulkOpen, setBulkOpen] = useState(false);
  const [specialOpen, setSpecialOpen] = useState(false);
  const [bulkForm, setBulkForm] = useState({
    fromDate: addDays(mondayISO(), 7),
    weeks: '4',
  });
  const [specialForm, setSpecialForm] = useState({
    teacherId: '',
    source: '3',
    date: toLocalISODate(new Date()),
    startTime: '14:00',
    endTime: '15:30',
    note: '',
  });

  useEffect(() => {
    if (!specialOpen) return;
    setSpecialForm({
      teacherId: '',
      source: '3',
      date: toLocalISODate(new Date()),
      startTime: '14:00',
      endTime: '15:30',
      note: '',
    });
  }, [specialOpen]);

  const branchesQuery = useQuery({ queryKey: ['branches'], queryFn: () => branchApi.list() });
  const selectedLessonQuery = useQuery({
    queryKey: ['lesson', lessonId],
    queryFn: () => fetchLesson(lessonId!),
    enabled: !!lessonId,
  });
  const selectedLesson = selectedLessonQuery.data;
  useEffect(() => {
    if (!selectedLesson) return;
    setWeekStart(mondayISO(selectedLesson.startAt));
    setBranchId(String(selectedLesson.branchId));
  }, [selectedLesson]);

  const selectLesson = (id: string | null) => {
    const next = new URLSearchParams(searchParams);
    next.delete('lessonId');
    if (id) next.set('openId', id);
    else next.delete('openId');
    setSearchParams(next, { replace: true });
  };
  const schedQuery = useQuery({
    queryKey: ['schedule-by-teacher', branchId, weekStart],
    queryFn: () => scheduleApi.byTeacher(branchId, weekStart),
  });
  const teachersQuery = useQuery({
    queryKey: ['teachers', branchId, 'special-lesson'],
    queryFn: () => teacherApi.list(branchId),
    enabled: specialOpen && !!branchId,
  });

  const bulkMut = useMutation({
    mutationFn: () =>
      scheduleApi.bulkGenerate({
        fromDate: bulkForm.fromDate,
        weeks: Number(bulkForm.weeks),
        branchId: branchId ?? undefined,
      }),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['schedule-by-teacher'] });
      setBulkOpen(false);
      toast.success(
        `已生成 ${data.generated} 节课，跳过 ${data.skipped}，已加入 ${data.rosterAdded} 名学员`,
      );
    },
    onError: (e: Error) => {
      toast.error(e instanceof ApiError ? formatApiErrorMessage(e.payload, e.message) : e.message);
    },
  });

  const specialMut = useMutation({
    mutationFn: () => {
      if (!branchId) throw new Error('请先选择校区');
      if (!specialForm.teacherId) throw new Error('请选择老师');
      const startAt = `${specialForm.date}T${specialForm.startTime}:00`;
      const endAt = `${specialForm.date}T${specialForm.endTime}:00`;
      return scheduleApi.createLesson({
        branchId,
        teacherId: specialForm.teacherId,
        startAt,
        endAt,
        source: Number(specialForm.source),
        note: specialForm.note || undefined,
      });
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['schedule-by-teacher'] });
      setSpecialOpen(false);
      toast.success('特殊课已创建');
    },
    onError: (e: Error) => {
      toast.error(e instanceof ApiError ? formatApiErrorMessage(e.payload, e.message) : e.message);
    },
  });

  const columns = schedQuery.data?.columns ?? [];
  // 仅显示有课的老师（按 plan R2 缓解）
  const activeColumns = columns.filter((c) => c.lessons.length > 0);

  return (
    <div className="space-y-4 pb-24" data-testid="weekly-schedule-page">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">周课表</h1>
          <p className="mt-1 text-sm text-muted-fg">按老师查看安排 · 选择课次查看详情与学员</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Button variant="ghost" onClick={() => setWeekStart(addDays(weekStart, -7))}>
            上一周
          </Button>
          <Input
            type="date"
            aria-label="课表日期"
            value={weekStart}
            onChange={(e) => {
              if (e.target.value) setWeekStart(mondayISO(e.target.value));
            }}
            className="w-40"
          />
          <Button variant="ghost" onClick={() => setWeekStart(addDays(weekStart, 7))}>
            下一周
          </Button>
          <SimpleSelect
            aria-label="课表校区"
            className="w-36"
            value={branchId ?? ''}
            onValueChange={setBranchId}
            options={(branchesQuery.data ?? []).map((b) => ({
              value: String(b.id),
              label: b.name,
            }))}
            placeholder="校区"
          />
          {canWriteLessons ? (
            <>
              <Button
                variant="secondary"
                onClick={() => setSpecialOpen(true)}
                disabled={!branchId}
                title={!branchId ? '请先选择校区' : undefined}
              >
                新建特殊课
              </Button>
              <Button onClick={() => setBulkOpen(true)}>批量排课</Button>
            </>
          ) : null}
        </div>
      </div>

      {schedQuery.isPending ? (
        <div
          role="status"
          aria-label="课表加载"
          className="rounded-xl border border-border bg-card/70 p-12 text-center text-muted-fg"
        >
          正在加载课表…
        </div>
      ) : schedQuery.isError ? (
        <div role="alert" className="rounded-xl border border-danger/20 bg-card/70 p-8 text-center">
          <p>课表加载失败，请稍后重试</p>
          <Button variant="secondary" className="mt-3" onClick={() => void schedQuery.refetch()}>
            重新加载
          </Button>
        </div>
      ) : activeColumns.length === 0 ? (
        <div className="rounded-xl border border-dashed border-border bg-card/70 p-12 text-center text-muted-fg">
          {canWriteLessons
            ? '本周暂无课次，可点击右上「批量排课」基于老师可用时段批量生成'
            : '本周暂无课次'}
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-border bg-card/70">
          <table className="w-full text-sm">
            <thead className="bg-muted">
              <tr>
                <th className="w-20 px-3 py-2 text-left">星期</th>
                {activeColumns.map((c) => (
                  <th key={String(c.teacherId)} className="px-3 py-2 text-left">
                    {c.teacherName}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {DOW_LABELS.map((label, dowIdx) => {
                const dow = dowIdx + 1;
                return (
                  <tr key={dow} className="border-t border-border">
                    <td className="px-3 py-2 font-medium">
                      {label}
                      <span className="mt-1 block text-xs font-normal text-muted-fg">
                        {addDays(weekStart, dowIdx).slice(5)}
                      </span>
                    </td>
                    {activeColumns.map((c) => {
                      const cells = c.lessons.filter((l) => l.dayOfWeek === dow);
                      return (
                        <td key={`${c.teacherId}-${dow}`} className="px-3 py-2 align-top">
                          {cells.length === 0 ? (
                            <span className="text-muted-fg">-</span>
                          ) : (
                            <div className="space-y-1">
                              {cells.map((l) => (
                                <button
                                  type="button"
                                  key={String(l.lessonId)}
                                  onClick={() => selectLesson(String(l.lessonId))}
                                  aria-label={`${c.teacherName} ${label} ${fmtTime(l.startMinute)}–${fmtTime(l.endMinute)} ${l.classRoomName ?? ''}，查看课次详情`}
                                  className="w-full rounded-xl border border-primary/10 bg-primary/5 px-3 py-3 text-left transition hover:border-primary/30 hover:bg-primary/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
                                >
                                  <div className="text-xs font-medium">
                                    {fmtTime(l.startMinute)}-{fmtTime(l.endMinute)}
                                    {l.source === 3
                                      ? ' · 补课'
                                      : l.source === 5
                                        ? ' · 考级'
                                        : l.source === 6
                                          ? ' · 比赛'
                                          : ''}
                                  </div>
                                  <div className="text-xs text-muted-fg">
                                    {l.classRoomName ?? '-'} · {l.studentCount}
                                    {l.capacity ? `/${l.capacity}` : ''}
                                  </div>
                                </button>
                              ))}
                            </div>
                          )}
                        </td>
                      );
                    })}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {lessonId ? (
        <LessonDetailSheet
          key={lessonId}
          lessonId={lessonId}
          open
          onOpenChange={(open) => {
            if (!open) selectLesson(null);
          }}
          onUpdated={() => {
            void qc.invalidateQueries({ queryKey: ['schedule-by-teacher'] });
            void qc.invalidateQueries({ queryKey: ['lesson', lessonId] });
            void qc.invalidateQueries({ queryKey: ['lesson-logs', lessonId] });
          }}
        />
      ) : null}

      <Dialog open={canWriteLessons && bulkOpen} onOpenChange={setBulkOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>批量排课</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <DialogDescription>
              基于已绑定分组的老师可用时段，按周生成未来课次。学员订阅自动入名单。
            </DialogDescription>
            <div>
              <Label>起始日期（周一）</Label>
              <Input
                className="mt-1"
                type="date"
                value={bulkForm.fromDate}
                onChange={(e) => setBulkForm({ ...bulkForm, fromDate: e.target.value })}
              />
            </div>
            <div>
              <Label>生成周数（1-8）</Label>
              <Input
                className="mt-1"
                type="number"
                min="1"
                max="8"
                value={bulkForm.weeks}
                onChange={(e) => setBulkForm({ ...bulkForm, weeks: e.target.value })}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setBulkOpen(false)}>
              取消
            </Button>
            <Button onClick={() => bulkMut.mutate()} disabled={bulkMut.isPending}>
              生成
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={canWriteLessons && specialOpen} onOpenChange={setSpecialOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>新建特殊课</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <DialogDescription>
              补课 / 考级 / 比赛不占用老师固定可用时段；若与已绑分组时段冲突将无法创建。
            </DialogDescription>
            <div>
              <Label>类型</Label>
              <SimpleSelect
                className="mt-1"
                aria-label="特殊课类型"
                value={specialForm.source}
                onValueChange={(v) => setSpecialForm({ ...specialForm, source: v })}
                options={SPECIAL_SOURCE_OPTIONS}
              />
            </div>
            <div>
              <Label>老师</Label>
              <SimpleSelect
                className="mt-1"
                aria-label="老师"
                value={specialForm.teacherId}
                onValueChange={(v) => setSpecialForm({ ...specialForm, teacherId: v })}
                options={(teachersQuery.data ?? []).map((t) => ({
                  value: String(t.id),
                  label: t.name || t.username,
                }))}
                placeholder="请选择老师"
              />
            </div>
            <div>
              <Label>日期</Label>
              <Input
                className="mt-1"
                type="date"
                value={specialForm.date}
                onChange={(e) => setSpecialForm({ ...specialForm, date: e.target.value })}
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label>开始</Label>
                <Input
                  className="mt-1"
                  type="time"
                  value={specialForm.startTime}
                  onChange={(e) => setSpecialForm({ ...specialForm, startTime: e.target.value })}
                />
              </div>
              <div>
                <Label>结束</Label>
                <Input
                  className="mt-1"
                  type="time"
                  value={specialForm.endTime}
                  onChange={(e) => setSpecialForm({ ...specialForm, endTime: e.target.value })}
                />
              </div>
            </div>
            <div>
              <Label>备注</Label>
              <Input
                className="mt-1"
                value={specialForm.note}
                onChange={(e) => setSpecialForm({ ...specialForm, note: e.target.value })}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setSpecialOpen(false)}>
              取消
            </Button>
            <Button
              onClick={() => specialMut.mutate()}
              disabled={specialMut.isPending || !specialForm.teacherId}
            >
              {specialMut.isPending ? '创建中…' : '创建'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
