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
        'flex flex-col items-center justify-center gap-3 py-12 text-center text-muted-fg',
        className,
      )}
    >
      {icon ?? <Folders weight="duotone" className="h-10 w-10 opacity-60" aria-hidden />}
      <h3 className="text-sm font-medium">{title}</h3>
      {description ? <p className="max-w-sm text-sm">{description}</p> : null}
      {action ? (
        <div className="pt-1">
          <Button variant="default" onClick={action.onClick}>
            {action.label}
          </Button>
        </div>
      ) : null}
    </div>
  );
}
