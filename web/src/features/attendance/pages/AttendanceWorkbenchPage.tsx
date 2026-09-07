import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DatePicker } from '@/components/ui/DatePicker';
import { SimpleSelect } from '@/components/ui/Select';
import { AttendanceRosterTable } from '@/features/attendance/components/AttendanceRosterTable';
import { PickupSelectDialog } from '@/features/attendance/components/PickupSelectDialog';
import { QrScanDialog } from '@/features/attendance/components/QrScanDialog';
import {
  checkIn,
  checkOut,
  fetchTodayRoster,
} from '@/features/attendance/api';
import type { DayPeriod, RosterItem } from '@/features/attendance/types';
import { useAuthStore } from '@/features/auth/store';
import { toast } from '@/lib/toast';

export function AttendanceWorkbenchPage(): JSX.Element {
  const branches = useAuthStore((s) => s.user?.branches ?? []);
  const [branchId, setBranchId] = useState(String(branches[0]?.id ?? 1));
  const [period, setPeriod] = useState<DayPeriod>('morning');
  const [date, setDate] = useState<Date | undefined>(new Date());
  const [pickupOpen, setPickupOpen] = useState(false);
  const [pickupMode, setPickupMode] = useState<'in' | 'out'>('in');
  const [activeItem, setActiveItem] = useState<RosterItem | null>(null);
  const [qrOpen, setQrOpen] = useState(false);
  const queryClient = useQueryClient();

  const rosterQuery = useQuery({
    queryKey: ['attendance-today', branchId, period, date?.toISOString().slice(0, 10)],
    queryFn: () =>
      fetchTodayRoster({
        branchId,
        period,
        date: date ? format(date, 'yyyy-MM-dd') : undefined,
      }),
    refetchInterval: 30_000,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['attendance-today'] });

  const checkInMutation = useMutation({
    mutationFn: (args: { item: RosterItem; guardianId?: number | string }) =>
      checkIn({
        lessonId: args.item.lessonId,
        studentId: args.item.studentId,
        method: 'manual',
        guardianId: args.guardianId,
      }),
    onSuccess: () => {
      toast.success('签到成功');
      invalidate();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const checkOutMutation = useMutation({
    mutationFn: (args: { item: RosterItem; guardianId?: number | string }) =>
      checkOut({
        attendanceId: args.item.attendanceId!,
        guardianId: args.guardianId,
      }),
    onSuccess: () => {
      toast.success('离园记录已保存');
      invalidate();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const openPickup = (item: RosterItem, mode: 'in' | 'out') => {
    setActiveItem(item);
    setPickupMode(mode);
    setPickupOpen(true);
  };

  const roster = rosterQuery.data;

  return (
    <div className="space-y-6" data-testid="attendance-workbench">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="font-serif text-xl font-semibold">签到工作台</h1>
          <p className="text-sm text-muted-fg">
            已入园 {roster?.checkedInCount ?? 0} / {roster?.totalExpected ?? 0}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" onClick={() => setQrOpen(true)}>
            扫码
          </Button>
        </div>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        <SimpleSelect
          aria-label="校区"
          value={branchId}
          onValueChange={setBranchId}
          options={branches.map((b) => ({ value: String(b.id), label: b.name }))}
        />
        <SimpleSelect
          aria-label="时段"
          value={period}
          onValueChange={(v) => setPeriod(v as DayPeriod)}
          options={[
            { value: 'morning', label: '上午' },
            { value: 'afternoon', label: '下午' },
            { value: 'evening', label: '晚上' },
          ]}
        />
        <DatePicker value={date} onChange={setDate} />
      </div>

      <AttendanceRosterTable
        items={roster?.items ?? []}
        onCheckIn={(item) => openPickup(item, 'in')}
        onCheckOut={(item) => openPickup(item, 'out')}
      />

      <PickupSelectDialog
        open={pickupOpen}
        item={activeItem}
        mode={pickupMode}
        onClose={() => setPickupOpen(false)}
        onConfirm={(guardianId) => {
          if (!activeItem) return;
          if (pickupMode === 'in') {
            checkInMutation.mutate({ item: activeItem, guardianId });
          } else {
            checkOutMutation.mutate({ item: activeItem, guardianId });
          }
          setPickupOpen(false);
        }}
      />

      <QrScanDialog open={qrOpen} onClose={() => setQrOpen(false)} />
    </div>
  );
}
