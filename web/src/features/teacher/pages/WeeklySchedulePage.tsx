import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';
import { useState } from 'react';
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
import { ApiError, formatApiErrorMessage } from '@/lib/api';
import { toast } from '@/lib/toast';
import { branchApi } from '@/features/student/api';
import { scheduleApi } from '../api';

const DOW_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];

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

function currentMondayISO(): string {
  const d = new Date();
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
  const [weekStart, setWeekStart] = useState(currentMondayISO());
  const [branchId, setBranchId] = useState<string | undefined>(undefined);
  const [bulkOpen, setBulkOpen] = useState(false);
  const [bulkForm, setBulkForm] = useState({
    fromDate: addDays(currentMondayISO(), 7),
    weeks: '4',
  });

  const branchesQuery = useQuery({ queryKey: ['branches'], queryFn: () => branchApi.list() });
  const schedQuery = useQuery({
    queryKey: ['schedule-by-teacher', branchId, weekStart],
    queryFn: () => scheduleApi.byTeacher(branchId, weekStart),
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
      toast.error(
        e instanceof ApiError ? formatApiErrorMessage(e.payload, e.message) : e.message,
      );
    },
  });

  const columns = schedQuery.data?.columns ?? [];
  // 仅显示有课的老师（按 plan R2 缓解）
  const activeColumns = columns.filter((c) => c.lessons.length > 0);

  return (
    <div className="space-y-4 pb-24" data-testid="weekly-schedule-page">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="font-serif text-xl font-semibold">周课表（按老师）</h1>
        <div className="flex flex-wrap items-center gap-2">
          <Button variant="ghost" onClick={() => setWeekStart(addDays(weekStart, -7))}>
            上一周
          </Button>
          <Input
            type="date"
            value={weekStart}
            onChange={(e) => setWeekStart(e.target.value)}
            className="w-40"
          />
          <Button variant="ghost" onClick={() => setWeekStart(addDays(weekStart, 7))}>
            下一周
          </Button>
          <SimpleSelect
            value={branchId}
            onValueChange={setBranchId}
            options={(branchesQuery.data ?? []).map((b) => ({
              value: String(b.id),
              label: b.name,
            }))}
            placeholder="校区"
          />
          <Button onClick={() => setBulkOpen(true)}>批量排课</Button>
        </div>
      </div>

      {activeColumns.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border bg-card p-12 text-center text-muted-fg">
          本周暂无课次，可点击右上「批量排课」基于老师可用时段批量生成
        </div>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-border bg-card">
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
                    <td className="px-3 py-2 font-medium">{label}</td>
                    {activeColumns.map((c) => {
                      const cells = c.lessons.filter((l) => l.dayOfWeek === dow);
                      return (
                        <td
                          key={`${c.teacherId}-${dow}`}
                          className="px-3 py-2 align-top"
                        >
                          {cells.length === 0 ? (
                            <span className="text-muted-fg">-</span>
                          ) : (
                            <div className="space-y-1">
                              {cells.map((l) => (
                                <div
                                  key={String(l.lessonId)}
                                  className="rounded-md border border-border bg-primary/5 px-2 py-1"
                                >
                                  <div className="text-xs font-medium">
                                    {fmtTime(l.startMinute)}-{fmtTime(l.endMinute)}
                                  </div>
                                  <div className="text-xs text-muted-fg">
                                    {l.classRoomName ?? '-'} ·{' '}
                                    {l.studentCount}
                                    {l.capacity ? `/${l.capacity}` : ''}
                                  </div>
                                </div>
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

      <Dialog open={bulkOpen} onOpenChange={setBulkOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>批量排课</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <p className="text-sm text-muted-fg">
              基于老师可用时段模板，按周生成未来课次。学员订阅自动入名单。
            </p>
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
    </div>
  );
}
