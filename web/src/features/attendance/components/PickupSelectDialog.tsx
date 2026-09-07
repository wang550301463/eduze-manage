import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DialogFrame } from '@/components/ui/Dialog';
import { Label } from '@/components/ui/Label';
import { SimpleSelect } from '@/components/ui/Select';
import { fetchStudentGuardians } from '@/features/attendance/api';
import type { RosterItem } from '@/features/attendance/types';

type Props = {
  open: boolean;
  item: RosterItem | null;
  mode: 'in' | 'out';
  onClose: () => void;
  onConfirm: (guardianId: string | undefined) => void;
};

export function PickupSelectDialog({ open, item, mode, onClose, onConfirm }: Props): JSX.Element {
  const [guardianId, setGuardianId] = useState<string>('');
  const { data: guardians = [], isLoading } = useQuery({
    queryKey: ['student-guardians', item?.studentId],
    queryFn: () => fetchStudentGuardians(item!.studentId, true),
    enabled: open && !!item,
  });

  const title = mode === 'in' ? '选择接送家长（入园）' : '选择接送家长（离园）';

  return (
    <DialogFrame
      open={open}
      onOpenChange={(v) => !v && onClose()}
      title={title}
      description={item ? `${item.studentName} · ${item.classGroupName}` : undefined}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            取消
          </Button>
          <Button
            onClick={() => onConfirm(guardianId || undefined)}
            disabled={isLoading}
          >
            确认
          </Button>
        </>
      }
    >
      <div className="space-y-2">
        <Label>接送家长</Label>
        <SimpleSelect
          aria-label="接送家长"
          value={guardianId}
          onValueChange={setGuardianId}
          placeholder="请选择家长"
          options={guardians.map((g) => ({
            value: String(g.id),
            label: `${g.name}（${g.relation || '家长'}）`,
          }))}
        />
      </div>
    </DialogFrame>
  );
}
