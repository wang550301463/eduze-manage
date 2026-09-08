import { CaretDown, CaretUp } from '@phosphor-icons/react';
import { cn } from '@/lib/cn';

export type KPICardProps = {
  title: string;
  value: number;
  suffix?: string;
  trend?: { value: number; direction: 'up' | 'down' };
  className?: string;
};

export function KPICard({
  title,
  value,
  suffix = '',
  trend,
  className,
}: KPICardProps): JSX.Element {
  return (
    <div className={cn('studio-panel p-5', className)}>
      <p className="text-sm text-muted-fg">{title}</p>
      <p className="mt-3 text-[30px] font-semibold tracking-tight tabular-nums text-foreground">
        {value.toLocaleString('zh-CN')}
        {suffix}
      </p>
      {trend ? (
        <p
          className={cn(
            'mt-1 flex items-center gap-0.5 text-xs',
            trend.direction === 'up' ? 'text-success' : 'text-error',
          )}
        >
          {trend.direction === 'up' ? (
            <CaretUp className="h-3 w-3" weight="fill" aria-hidden />
          ) : (
            <CaretDown className="h-3 w-3" weight="fill" aria-hidden />
          )}
          {trend.value}%
        </p>
      ) : null}
    </div>
  );
}
