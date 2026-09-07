import CountUp from 'react-countup';
import { CaretDown, CaretUp } from '@phosphor-icons/react';
import { cn } from '@/lib/cn';

export type KPICardProps = {
  title: string;
  value: number;
  suffix?: string;
  trend?: { value: number; direction: 'up' | 'down' };
  className?: string;
};

export function KPICard({ title, value, suffix = '', trend, className }: KPICardProps): JSX.Element {
  return (
    <div
      className={cn(
        'rounded-lg border border-border bg-background p-4 shadow-sm',
        className,
      )}
    >
      <p className="text-sm text-muted-fg">{title}</p>
      <p className="mt-1 font-serif text-2xl font-semibold text-foreground">
        <CountUp end={value} duration={1} separator="," />
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
