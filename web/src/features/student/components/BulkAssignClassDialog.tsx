import { Button } from '@/components/ui/Button';
import { DialogFrame } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { useState } from 'react';
import { toast } from '@/lib/toast';
import { studentApi } from '../api';

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  studentIds: string[];
  mode: 'assign' | 'transfer';
  onDone: () => void;
};

/** 阶段 G3 前为占位：调用接口将提示尚未启用 */
export function BulkAssignClassDialog({
  open,
  onOpenChange,
  studentIds,
  mode,
  onDone,
}: Props): JSX.Element {
  const [classGroupId, setClassGroupId] = useState('');

  const submit = async () => {
    try {
      await studentApi.bulkAssignClass({
        studentIds,
        classGroupId,
      });
      toast.success('批量分班成功');
      onDone();
      onOpenChange(false);
    } catch (e) {
      toast.info(e instanceof Error ? e.message : '阶段 G 启用后可用');
    }
  };

  return (
    <DialogFrame
      open={open}
      onOpenChange={onOpenChange}
      title={mode === 'assign' ? '批量分班' : '批量调班'}
      description={`已选 ${studentIds.length} 名学员（班级 API 阶段 G 接通）`}
      footer={
        <Button onClick={() => void submit()} disabled={!classGroupId}>
          确认
        </Button>
      }
    >
      <div>
        <Label>班级 ID（占位）</Label>
        <Input
          className="mt-1"
          value={classGroupId}
          onChange={(e) => setClassGroupId(e.target.value)}
          placeholder="G 阶段后改为下拉选择"
        />
      </div>
    </DialogFrame>
  );
}
