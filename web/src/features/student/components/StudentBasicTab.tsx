import type { Student } from '../types';
import { calcAge, genderLabel, statusLabel } from '../utils';

type Props = { student: Student; onEdit: () => void };

export function StudentBasicTab({ student, onEdit }: Props): JSX.Element {
  return (
    <div className="space-y-4 text-sm">
      <div className="flex justify-end">
        <button type="button" className="text-primary hover:underline" onClick={onEdit}>
          编辑
        </button>
      </div>
      <dl className="grid grid-cols-2 gap-3">
        <Item label="入园编号" value={student.enrollNo} />
        <Item label="姓名" value={student.name} />
        <Item label="性别" value={genderLabel(student.gender)} />
        <Item label="年龄" value={calcAge(student.birthday)} />
        <Item label="校区" value={student.branchName ?? '-'} />
        <Item label="状态" value={statusLabel(student.status)} />
        <Item label="生日" value={student.birthday ?? '-'} />
        <Item label="入园日期" value={student.enrollDate ?? '-'} />
        <Item label="紧急联系人" value={student.emergencyContact ?? '-'} />
        <Item label="紧急电话" value={student.emergencyPhone ?? '-'} />
        <Item label="过敏史" value={student.allergy ?? '-'} className="col-span-2" />
        <Item label="健康备注" value={student.healthNote ?? '-'} className="col-span-2" />
      </dl>
    </div>
  );
}

function Item({
  label,
  value,
  className,
}: {
  label: string;
  value: string;
  className?: string;
}): JSX.Element {
  return (
    <div className={className}>
      <dt className="text-muted-fg">{label}</dt>
      <dd className="font-medium">{value}</dd>
    </div>
  );
}
