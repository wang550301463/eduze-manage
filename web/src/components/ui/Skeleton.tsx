import { cn } from '@/lib/cn';

export type SkeletonProps = React.HTMLAttributes<HTMLDivElement>;

export function Skeleton({ className, ...props }: SkeletonProps): JSX.Element {
  return (
    <div className={cn('animate-pulse rounded-md bg-muted', className)} {...props} />
  );
}
