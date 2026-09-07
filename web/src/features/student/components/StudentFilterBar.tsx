import { Button } from '@/components/ui/Button';
import { Checkbox } from '@/components/ui/Checkbox';
import { Label } from '@/components/ui/Label';
import { SimpleSelect } from '@/components/ui/Select';
import type { Branch } from '../types';

type FilterState = {
  branchId?: string;
  status?: string;
  lowBalance?: string;
};

type Props = {
  filters: FilterState;
  branches: Branch[];
  onChange: (patch: Partial<FilterState>) => void;
  onReset: () => void;
};

/** Radix Select 不允许 value=""，用哨兵表示「全部」 */
const ALL = '__all__';

const STATUS_OPTIONS = [
  { value: ALL, label: '全部状态' },
  { value: '1', label: '在读' },
  { value: '2', label: '暂停' },
  { value: '3', label: '退学' },
];

export function StudentFilterBar({ filters, branches, onChange, onReset }: Props): JSX.Element {
  return (
    <div className="flex flex-wrap items-end gap-3">
      <div className="min-w-[140px]">
        <Label>校区</Label>
        <SimpleSelect
          aria-label="校区"
          value={filters.branchId || ALL}
          onValueChange={(v) => onChange({ branchId: v === ALL ? undefined : v })}
          options={[
            { value: ALL, label: '全部校区' },
            ...branches.map((b) => ({ value: String(b.id), label: b.name })),
          ]}
        />
      </div>
      <div className="min-w-[140px]">
        <Label>状态</Label>
        <SimpleSelect
          aria-label="状态"
          value={filters.status || ALL}
          onValueChange={(v) => onChange({ status: v === ALL ? undefined : v })}
          options={STATUS_OPTIONS}
        />
      </div>
      <div className="flex items-center gap-2 pb-2">
        <Checkbox
          id="low-balance"
          checked={filters.lowBalance === '5'}
          onCheckedChange={(c) => onChange({ lowBalance: c === true ? '5' : undefined })}
          aria-label="低余额"
        />
        <Label htmlFor="low-balance">课时 ≤5</Label>
      </div>
      <Button type="button" variant="ghost" onClick={onReset}>
        重置
      </Button>
    </div>
  );
}
