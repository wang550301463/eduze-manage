import { Drawer as DrawerPrimitive } from 'vaul';
import { type ComponentPropsWithoutRef, type HTMLAttributes, type ReactNode } from 'react';
import { cn } from '@/lib/cn';

export const Drawer = ({
  shouldScaleBackground = true,
  ...props
}: ComponentPropsWithoutRef<typeof DrawerPrimitive.Root>): JSX.Element => (
  <DrawerPrimitive.Root shouldScaleBackground={shouldScaleBackground} {...props} />
);

export const DrawerTrigger = DrawerPrimitive.Trigger;
export const DrawerClose = DrawerPrimitive.Close;

export function DrawerOverlay({
  className,
  ...props
}: ComponentPropsWithoutRef<typeof DrawerPrimitive.Overlay>): JSX.Element {
  return (
    <DrawerPrimitive.Overlay
      className={cn('fixed inset-0 z-50 bg-foreground/40 backdrop-blur-sm', className)}
      {...props}
    />
  );
}

export function DrawerContent({
  className,
  children,
  ...props
}: ComponentPropsWithoutRef<typeof DrawerPrimitive.Content>): JSX.Element {
  return (
    <DrawerPrimitive.Portal>
      <DrawerOverlay />
      <DrawerPrimitive.Content
        className={cn(
          'fixed inset-x-0 bottom-0 z-50 mt-24 flex h-auto flex-col rounded-t-xl border border-border bg-background',
          className,
        )}
        {...props}
      >
        <div className="mx-auto mt-4 h-1.5 w-12 shrink-0 rounded-full bg-muted" />
        {children}
      </DrawerPrimitive.Content>
    </DrawerPrimitive.Portal>
  );
}

export function DrawerHeader({ className, ...props }: HTMLAttributes<HTMLDivElement>): JSX.Element {
  return <div className={cn('grid gap-1.5 p-4 text-center sm:text-left', className)} {...props} />;
}

export function DrawerTitle({
  className,
  ...props
}: ComponentPropsWithoutRef<typeof DrawerPrimitive.Title>): JSX.Element {
  return <DrawerPrimitive.Title className={cn('text-lg font-semibold', className)} {...props} />;
}

export function DrawerFooter({ className, ...props }: HTMLAttributes<HTMLDivElement>): JSX.Element {
  return <div className={cn('mt-auto flex flex-col gap-2 p-4', className)} {...props} />;
}

type DrawerFrameProps = {
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  trigger?: ReactNode;
  title: string;
  children?: ReactNode;
};

export function DrawerFrame({
  open,
  onOpenChange,
  trigger,
  title,
  children,
}: DrawerFrameProps): JSX.Element {
  return (
    <Drawer open={open} onOpenChange={onOpenChange}>
      {trigger ? <DrawerTrigger asChild>{trigger}</DrawerTrigger> : null}
      <DrawerContent>
        <DrawerHeader>
          <DrawerTitle>{title}</DrawerTitle>
        </DrawerHeader>
        <div className="p-4 pt-0">{children}</div>
      </DrawerContent>
    </Drawer>
  );
}
