import { Button } from '@/components/ui/Button';
import { DialogFrame } from '@/components/ui/Dialog';
import type { ConflictReport } from '@/features/lesson/types';

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  report: ConflictReport | null;
  onConfirm: () => void;
};

export function ConflictWarningDialog({ open, onOpenChange, report, onConfirm }: Props): JSX.Element {
  return (
    <DialogFrame
      open={open}
      onOpenChange={onOpenChange}
      title="时间冲突提醒"
      description="检测到以下冲突，仍可继续调课（软警告）。"
      footer={
        <>
          <Button variant="ghost" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button onClick={onConfirm}>仍然继续</Button>
        </>
      }
    >
      <ul className="list-disc space-y-1 pl-5 text-sm">
        {report?.teacher ? <li>教师：{report.teacher.classGroupName}</li> : null}
        {report?.classRoom ? <li>画室：{report.classRoom.classRoomName}</li> : null}
        {report?.classGroup ? <li>班级：{report.classGroup.classGroupName}</li> : null}
      </ul>
    </DialogFrame>
  );
}
