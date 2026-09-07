import * as CheckboxPrimitive from '@radix-ui/react-checkbox';
import { Check } from '@phosphor-icons/react';
import { forwardRef, type ComponentPropsWithoutRef } from 'react';
import { cn } from '@/lib/cn';

export type CheckboxProps = ComponentPropsWithoutRef<typeof CheckboxPrimitive.Root>;

export const Checkbox = forwardRef<HTMLButtonElement, CheckboxProps>(
  ({ className, ...props }, ref) => (
    <CheckboxPrimitive.Root
      ref={ref}
      className={cn(
        'peer h-4 w-4 shrink-0 rounded-sm border border-border bg-background',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40',
        'disabled:cursor-not-allowed disabled:opacity-50',
        'data-[state=checked]:bg-primary data-[state=checked]:text-primary-fg data-[state=checked]:border-primary',
        className,
      )}
      {...props}
    >
      <CheckboxPrimitive.Indicator className="flex items-center justify-center text-current">
        <Check weight="bold" className="h-3 w-3" aria-hidden />
      </CheckboxPrimitive.Indicator>
    </CheckboxPrimitive.Root>
  ),
);
Checkbox.displayName = 'Checkbox';
