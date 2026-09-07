import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import type { UseFormRegister } from 'react-hook-form';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { studentApi } from '../api';
import type { StudentFormValues } from '../schemas';

type Props = {
  register: UseFormRegister<StudentFormValues>;
};

export function GuardianSubForm({ register }: Props): JSX.Element {
  const [lookup, setLookup] = useState('');
  const { data } = useQuery({
    queryKey: ['guardian-lookup', lookup],
    queryFn: () => studentApi.searchGuardians(lookup),
    enabled: lookup.length >= 3,
  });

  return (
    <div className="space-y-3">
      <div>
        <Label>按手机号查找已有家长</Label>
        <Input
          className="mt-1"
          placeholder="输入至少 3 位"
          value={lookup}
          onChange={(e) => setLookup(e.target.value)}
        />
        {data?.records?.length ? (
          <ul className="mt-2 max-h-24 overflow-auto rounded border border-border text-sm">
            {data.records.map((g) => (
              <li key={g.id} className="cursor-pointer px-2 py-1 hover:bg-muted">
                {g.name} · {g.phone}
              </li>
            ))}
          </ul>
        ) : null}
      </div>
      <div>
        <Label>家长姓名</Label>
        <Input className="mt-1" {...register('guardianName')} />
      </div>
      <div>
        <Label>家长手机</Label>
        <Input className="mt-1" {...register('guardianPhone')} />
      </div>
      <div>
        <Label>关系</Label>
        <Input className="mt-1" {...register('guardianRelation')} />
      </div>
    </div>
  );
}
