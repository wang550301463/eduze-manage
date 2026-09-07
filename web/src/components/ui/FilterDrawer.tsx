import type { ReactNode } from 'react';
import { Button } from '@/components/ui/Button';
import {
  Drawer,
  DrawerClose,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from '@/components/ui/Drawer';

type FilterDrawerProps = {
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  trigger?: ReactNode;
  title: string;
  children: ReactNode;
  onReset?: () => void;
  onApply?: () => void;
};

/** 移动端通用筛选抽屉：标题 / 筛选项 / 底部重置 + 应用 */
export function FilterDrawer({
  open,
  onOpenChange,
  trigger,
  title,
  children,
  onReset,
  onApply,
}: FilterDrawerProps): JSX.Element {
  return (
    <Drawer open={open} onOpenChange={onOpenChange}>
      {trigger}
      <DrawerContent className="max-h-[85vh]">
        <DrawerHeader className="border-b border-border text-left">
          <DrawerTitle>{title}</DrawerTitle>
        </DrawerHeader>
        <div className="flex-1 overflow-y-auto px-4 py-4">{children}</div>
        <DrawerFooter className="flex-row gap-2 border-t border-border">
          <DrawerClose asChild>
            <Button type="button" variant="secondary" className="flex-1" onClick={onReset}>
              重置
            </Button>
          </DrawerClose>
          <DrawerClose asChild>
            <Button type="button" className="flex-1" onClick={onApply}>
              应用
            </Button>
          </DrawerClose>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  );
}
