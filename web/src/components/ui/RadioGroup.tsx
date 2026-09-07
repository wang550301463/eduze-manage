import * as RadioGroupPrimitive from '@radix-ui/react-radio-group';
import { forwardRef, type ComponentPropsWithoutRef } from 'react';
import { cn } from '@/lib/cn';

export const RadioGroup = forwardRef<
  HTMLDivElement,
  ComponentPropsWithoutRef<typeof RadioGroupPrimitive.Root>
>(({ className, ...props }, ref) => (
  <RadioGroupPrimitive.Root ref={ref} className={cn('grid gap-2', className)} {...props} />
));
RadioGroup.displayName = 'RadioGroup';

export const RadioGroupItem = forwardRef<
  HTMLButtonElement,
  ComponentPropsWithoutRef<typeof RadioGroupPrimitive.Item>
>(({ className, ...props }, ref) => (
  <RadioGroupPrimitive.Item
    ref={ref}
    className={cn(
      'aspect-square h-4 w-4 rounded-full border border-border text-primary',
      'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40',
      'disabled:cursor-not-allowed disabled:opacity-50',
      className,
    )}
    {...props}
  >
    <RadioGroupPrimitive.Indicator className="flex items-center justify-center">
      <span className="h-2 w-2 rounded-full bg-primary" />
    </RadioGroupPrimitive.Indicator>
  </RadioGroupPrimitive.Item>
));
RadioGroupItem.displayName = 'RadioGroupItem';
