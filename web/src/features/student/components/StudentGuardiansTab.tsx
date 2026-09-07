import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { StudentQrCodePanel } from '@/features/attendance/components/StudentQrCodePanel';
import { fetchStudentGuardians } from '@/features/attendance/api';

type Props = {
  studentId: string;
};

export function StudentGuardiansTab({ studentId }: Props): JSX.Element {
  const [qrGuardianId, setQrGuardianId] = useState<string | null>(null);
  const { data: guardians = [], isLoading } = useQuery({
    queryKey: ['student-guardians-all', studentId],
    queryFn: () => fetchStudentGuardians(studentId, false),
    enabled: Boolean(studentId),
  });

  if (isLoading) {
    return <Skeleton className="h-24 w-full" />;
  }

  return (
    <div className="space-y-4">
      <ul className="divide-y divide-border text-sm">
        {guardians.map((g) => (
          <li key={g.id} className="flex items-center justify-between py-2">
            <div>
              <p className="font-medium">{g.name}</p>
              <p className="text-xs text-muted-fg">
                {g.relation} · {g.phone}
                {g.canPickup ? '' : ' · 不可接送'}
              </p>
            </div>
            <Button size="sm" variant="ghost" onClick={() => setQrGuardianId(String(g.id))}>
              显示二维码
            </Button>
          </li>
        ))}
      </ul>
      {qrGuardianId ? (
        <StudentQrCodePanel
          guardianId={qrGuardianId}
          guardianName={guardians.find((g) => g.id === qrGuardianId)?.name ?? ''}
        />
      ) : null}
    </div>
  );
}
