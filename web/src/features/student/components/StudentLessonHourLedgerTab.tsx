import { useQuery } from '@tanstack/react-query';
import { EmptyState } from '@/components/ui/EmptyState';
import { Skeleton } from '@/components/ui/Skeleton';
import { cn } from '@/lib/cn';
import { studentApi } from '../api';

type Props = { studentId: string };

const EVENT_LABELS: Record<string, string> = {
  ATTEND: '出勤扣减',
  ABSENT_DEDUCT: '缺勤扣减',
  MAKEUP: '补课入账',
  ADJUST: '调账',
  VOID: '冲正',
};

function formatOccurredAt(iso: string): string {
  try {
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return iso;
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hh = String(d.getHours()).padStart(2, '0');
    const mm = String(d.getMinutes()).padStart(2, '0');
    return `${y}-${m}-${day} ${hh}:${mm}`;
  } catch {
    return iso;
  }
}

export function StudentLessonHourLedgerTab({ studentId }: Props): JSX.Element {
  const { data = [], isLoading, isError } = useQuery({
    queryKey: ['student-lesson-hour-ledger', studentId],
    queryFn: () => studentApi.listLessonHourLedger(studentId),
  });

  if (isLoading) {
    return (
      <div className="space-y-2">
        <Skeleton className="h-12 w-full" />
        <Skeleton className="h-12 w-full" />
        <Skeleton className="h-12 w-full" />
      </div>
    );
  }

  if (isError) {
    return <EmptyState title="无法加载课时流水" description="请稍后重试" />;
  }

  if (data.length === 0) {
    return <EmptyState title="暂无课时流水" description="出勤扣减、调账等记录会出现在这里" />;
  }

  return (
    <div className="space-y-3">
      <p className="text-xs text-muted-fg">只读审计流水；余额以课时包为准。</p>
      <ul className="space-y-2">
        {data.map((row) => {
          const delta = row.minutesDelta;
          const positive = delta > 0;
          return (
            <li
              key={row.id}
              className="rounded-lg border border-border px-3 py-2 text-sm"
            >
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="font-medium">
                    {EVENT_LABELS[row.eventType] ?? row.eventType}
                  </p>
                  <p className="text-xs text-muted-fg">{formatOccurredAt(row.occurredAt)}</p>
                </div>
                <span
                  className={cn(
                    'shrink-0 font-medium tabular-nums',
                    positive ? 'text-primary' : 'text-danger',
                  )}
                >
                  {positive ? '+' : ''}
                  {delta} 分钟
                </span>
              </div>
              <p className="mt-1 text-xs text-muted-fg">
                变更后余额：
                {row.balanceAfterMinutes != null ? `${row.balanceAfterMinutes} 分钟` : '—'}
                {row.note ? ` · ${row.note}` : ''}
              </p>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
