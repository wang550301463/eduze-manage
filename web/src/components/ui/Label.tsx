import * as LabelPrimitive from '@radix-ui/react-label';
import { forwardRef, type ComponentPropsWithoutRef } from 'react';
import { cn } from '@/lib/cn';

export type LabelProps = ComponentPropsWithoutRef<typeof LabelPrimitive.Root>;

export const Label = forwardRef<HTMLLabelElement, LabelProps>(
  ({ className, ...props }, ref) => (
    <LabelPrimitive.Root
      ref={ref}
      className={cn('text-sm font-medium leading-none text-foreground', className)}
      {...props}
    />
  ),
);
Label.displayName = 'Label';
