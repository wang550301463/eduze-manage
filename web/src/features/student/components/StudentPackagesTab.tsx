import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { useState } from 'react';
import { cn } from '@/lib/cn';
import { toast } from '@/lib/toast';
import { studentApi } from '../api';

type Props = { studentId: string };

export function StudentPackagesTab({ studentId }: Props): JSX.Element {
  const qc = useQueryClient();
  const { data = [] } = useQuery({
    queryKey: ['student-packages', studentId],
    queryFn: () => studentApi.packages(studentId),
  });
  const [total, setTotal] = useState('20');
  const [remaining, setRemaining] = useState('20');

  const create = useMutation({
    mutationFn: () =>
      studentApi.createPackage(studentId, {
        totalLessons: Number(total),
        remainingLessons: Number(remaining),
      }),
    onSuccess: () => {
      toast.success('课时包已创建');
      qc.invalidateQueries({ queryKey: ['student-packages', studentId] });
      qc.invalidateQueries({ queryKey: ['students'] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <div className="space-y-4">
      <ul className="space-y-2">
        {data.map((p) => (
          <li
            key={p.id}
            className={cn(
              'rounded-lg border border-border px-3 py-2 text-sm',
              p.alertLow && 'border-danger/40 bg-danger/5',
            )}
          >
            <p className="font-medium">
              剩余 {p.remainingLessons} / {p.totalLessons} 节
              {p.alertLow ? <span className="ml-2 text-danger">余额偏低</span> : null}
            </p>
            <p className="text-muted-fg">
              到期：{p.expireDate ?? '无'} {p.note ? `· ${p.note}` : ''}
            </p>
          </li>
        ))}
      </ul>
      <div className="space-y-2 rounded-lg border border-border p-3">
        <p className="text-sm font-medium">新增课时包</p>
        <div className="grid grid-cols-2 gap-2">
          <div>
            <Label>总课时</Label>
            <Input className="mt-1" value={total} onChange={(e) => setTotal(e.target.value)} />
          </div>
          <div>
            <Label>剩余</Label>
            <Input className="mt-1" value={remaining} onChange={(e) => setRemaining(e.target.value)} />
          </div>
        </div>
        <Button className="w-full" onClick={() => create.mutate()}>
          创建
        </Button>
      </div>
    </div>
  );
}
