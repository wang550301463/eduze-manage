import { forwardRef, type TextareaHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

export type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement>;

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, ...props }, ref) => (
    <textarea
      ref={ref}
      className={cn(
        'flex min-h-[80px] w-full rounded-md border border-border bg-background px-3 py-2 text-sm',
        'placeholder:text-muted-fg',
        'focus-visible:ring-2 focus-visible:ring-primary/40 focus-visible:ring-offset-0',
        'disabled:cursor-not-allowed disabled:opacity-50',
        'aria-invalid:border-error aria-invalid:ring-error/40',
        className,
      )}
      {...props}
    />
  ),
);
Textarea.displayName = 'Textarea';
