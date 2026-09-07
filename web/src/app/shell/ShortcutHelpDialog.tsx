import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/Dialog';
import { formatShortcutLabel } from '@/lib/kbd';

const shortcuts = [
  { keys: 'mod+k', description: '打开全局搜索' },
  { keys: 'mod+n', description: '新建（当前模块）' },
  { keys: '?', description: '显示快捷键帮助' },
  { keys: 'escape', description: '关闭弹窗 / 面板' },
] as const;

type ShortcutHelpDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

export function ShortcutHelpDialog({ open, onOpenChange }: ShortcutHelpDialogProps): JSX.Element {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>键盘快捷键</DialogTitle>
          <DialogDescription>在任意页面可使用以下快捷键（输入框内部分快捷键除外）</DialogDescription>
        </DialogHeader>
        <ul className="space-y-3 py-2">
          {shortcuts.map((item) => (
            <li key={item.keys} className="flex items-center justify-between gap-4 text-sm">
              <span className="text-muted-fg">{item.description}</span>
              <kbd className="rounded border border-border bg-muted px-2 py-1 font-mono text-xs">
                {formatShortcutLabel(item.keys)}
              </kbd>
            </li>
          ))}
        </ul>
      </DialogContent>
    </Dialog>
  );
}
