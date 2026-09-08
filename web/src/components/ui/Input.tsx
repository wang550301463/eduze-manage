import { forwardRef, type InputHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

export type InputProps = InputHTMLAttributes<HTMLInputElement>;

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, type = 'text', ...props }, ref) => (
    <input
      ref={ref}
      type={type}
      className={cn(
        'flex h-9 w-full rounded-[10px] border border-border bg-white/75 shadow-sm px-3 text-sm',
        'placeholder:text-muted-fg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30',
        'disabled:cursor-not-allowed disabled:opacity-50',
        'aria-invalid:border-error aria-invalid:ring-error/40',
        className,
      )}
      {...props}
    />
  ),
);
Input.displayName = 'Input';
