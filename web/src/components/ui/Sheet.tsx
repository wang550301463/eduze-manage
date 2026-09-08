import * as DialogPrimitive from '@radix-ui/react-dialog';
import { X } from '@phosphor-icons/react';
import { cva, type VariantProps } from 'class-variance-authority';
import {
  forwardRef,
  type ComponentPropsWithoutRef,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cn } from '@/lib/cn';

const sheetVariants = cva(
  'fixed z-50 flex flex-col gap-4 border border-white/80 bg-white/95 shadow-2xl backdrop-blur-2xl transition ease-in-out data-[state=closed]:duration-300 data-[state=open]:duration-300',
  {
    variants: {
      side: {
        right:
          'inset-y-0 right-0 h-full w-full rounded-l-xl sm:w-[480px] data-[state=closed]:animate-slide-out-to-right data-[state=open]:animate-slide-in-from-right',
        left: 'inset-y-0 left-0 h-full w-full rounded-r-xl sm:w-[480px]',
      },
    },
    defaultVariants: { side: 'right' },
  },
);

export const Sheet = DialogPrimitive.Root;
export const SheetTrigger = DialogPrimitive.Trigger;
export const SheetClose = DialogPrimitive.Close;

export const SheetOverlay = forwardRef<
  HTMLDivElement,
  ComponentPropsWithoutRef<typeof DialogPrimitive.Overlay>
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Overlay
    ref={ref}
    className={cn('fixed inset-0 z-50 bg-foreground/20 backdrop-blur-sm', className)}
    {...props}
  />
));
SheetOverlay.displayName = 'SheetOverlay';

type SheetContentProps = ComponentPropsWithoutRef<typeof DialogPrimitive.Content> &
  VariantProps<typeof sheetVariants>;

export const SheetContent = forwardRef<HTMLDivElement, SheetContentProps>(
  ({ side = 'right', className, children, ...props }, ref) => (
    <DialogPrimitive.Portal>
      <SheetOverlay />
      <DialogPrimitive.Content
        ref={ref}
        aria-describedby={undefined}
        data-side={side}
        className={cn(sheetVariants({ side }), className)}
        {...props}
      >
        {children}
        <DialogPrimitive.Close
          className="absolute right-4 top-4 rounded-sm opacity-70 hover:opacity-100"
          aria-label="关闭"
        >
          <X className="h-4 w-4" />
        </DialogPrimitive.Close>
      </DialogPrimitive.Content>
    </DialogPrimitive.Portal>
  ),
);
SheetContent.displayName = 'SheetContent';

export function SheetHeader({ className, ...props }: HTMLAttributes<HTMLDivElement>): JSX.Element {
  return (
    <div className={cn('flex flex-col gap-1.5 border-b border-border p-6', className)} {...props} />
  );
}

export function SheetBody({ className, ...props }: HTMLAttributes<HTMLDivElement>): JSX.Element {
  return <div className={cn('flex-1 overflow-y-auto p-6', className)} {...props} />;
}

export function SheetFooter({ className, ...props }: HTMLAttributes<HTMLDivElement>): JSX.Element {
  return (
    <div className={cn('mt-auto flex gap-2 border-t border-border p-6', className)} {...props} />
  );
}

export function SheetTitle({
  className,
  ...props
}: ComponentPropsWithoutRef<typeof DialogPrimitive.Title>): JSX.Element {
  return <DialogPrimitive.Title className={cn('text-lg font-semibold', className)} {...props} />;
}

type SheetFrameProps = {
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  trigger?: ReactNode;
  title: string;
  children?: ReactNode;
  footer?: ReactNode;
};

export function SheetFrame({
  open,
  onOpenChange,
  trigger,
  title,
  children,
  footer,
}: SheetFrameProps): JSX.Element {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      {trigger ? <SheetTrigger asChild>{trigger}</SheetTrigger> : null}
      <SheetContent>
        <SheetHeader>
          <SheetTitle>{title}</SheetTitle>
        </SheetHeader>
        <SheetBody>{children}</SheetBody>
        {footer ? <SheetFooter>{footer}</SheetFooter> : null}
      </SheetContent>
    </Sheet>
  );
}
