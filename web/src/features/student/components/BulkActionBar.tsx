import { Button } from '@/components/ui/Button';

type Props = {
  count: number;
  onAssign: () => void;
  onTransfer: () => void;
  onClear: () => void;
};

export function BulkActionBar({ count, onAssign, onTransfer, onClear }: Props): JSX.Element {
  if (count === 0) return <></>;
  return (
    <div className="fixed bottom-0 left-0 right-0 z-30 border-t border-border bg-background/95 px-4 py-3 shadow-lg backdrop-blur md:left-64">
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-2">
        <span className="text-sm">已选 {count} 人</span>
        <div className="flex gap-2">
          <Button size="sm" variant="secondary" onClick={onAssign}>
            批量分班
          </Button>
          <Button size="sm" variant="ghost" onClick={onTransfer}>
            批量调班
          </Button>
          <Button size="sm" variant="ghost" onClick={onClear}>
            取消
          </Button>
        </div>
      </div>
    </div>
  );
}
