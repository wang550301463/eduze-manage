import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/ui/EmptyState';
import { Skeleton } from '@/components/ui/Skeleton';
import { StudentQrCodePanel } from '@/features/attendance/components/StudentQrCodePanel';
import { fetchStudentGuardians } from '@/features/attendance/api';
import { cn } from '@/lib/cn';
import { useHasPermission } from '@/lib/permissions';

type Props = {
  studentId: string;
  guardianId?: string | null;
};

export function StudentGuardiansTab({ studentId, guardianId }: Props): JSX.Element {
  const [qrGuardianId, setQrGuardianId] = useState<string | null>(null);
  const canRead = useHasPermission('guardian:read');
  const {
    data: guardians = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['student-guardians-all', studentId],
    queryFn: () => fetchStudentGuardians(studentId, false),
    enabled: canRead && Boolean(studentId),
  });

  if (!canRead) return <EmptyState title="暂无查看家长信息的权限" />;

  if (isLoading) {
    return <Skeleton className="h-24 w-full" />;
  }

  if (isError) {
    return (
      <div className="space-y-3 rounded-lg border border-border p-4">
        <p role="alert" className="text-sm text-error">
          {error instanceof Error ? error.message : '无法加载家长信息'}
        </p>
        <Button variant="secondary" size="sm" onClick={() => void refetch()}>
          重试
        </Button>
      </div>
    );
  }

  if (guardians.length === 0) return <EmptyState title="暂无家长信息" />;

  return (
    <div className="space-y-4">
      <ul className="divide-y divide-border text-sm">
        {guardians.map((g) => (
          <li
            key={g.id}
            aria-current={guardianId === String(g.id) ? true : undefined}
            className={cn(
              'flex items-center justify-between py-2',
              guardianId === String(g.id) && 'rounded-lg bg-primary/10 px-3 ring-1 ring-primary/20',
            )}
          >
            <div>
              <p className="font-medium">{g.name}</p>
              {guardianId === String(g.id) ? (
                <p className="text-xs text-primary">搜索匹配</p>
              ) : null}
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
