import { Folders } from '@phosphor-icons/react';
import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';
import { Button } from './Button';

export type EmptyStateProps = {
  title: string;
  description?: string;
  action?: { label: string; onClick: () => void };
  icon?: ReactNode;
  className?: string;
};

export function EmptyState({
  title,
  description,
  action,
  icon,
  className,
}: EmptyStateProps): JSX.Element {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-border p-12 text-center',
        className,
      )}
    >
      {icon ?? <Folders weight="duotone" className="h-12 w-12 text-muted-fg" aria-hidden />}
      <h3 className="font-serif text-lg font-medium text-foreground">{title}</h3>
      {description ? <p className="max-w-sm text-sm text-muted-fg">{description}</p> : null}
      {action ? (
        <Button variant="default" onClick={action.onClick}>
          {action.label}
        </Button>
      ) : null}
    </div>
  );
}
