import { cn } from '@/lib/cn';

type Props = {
  steps: string[];
  current: number;
};

export function SectionStepper({ steps, current }: Props): JSX.Element {
  return (
    <ol className="mb-4 flex gap-2 text-xs">
      {steps.map((label, i) => (
        <li
          key={label}
          className={cn(
            'rounded-full px-2 py-1',
            i === current ? 'bg-primary text-primary-fg' : 'bg-muted text-muted-fg',
          )}
        >
          {label}
        </li>
      ))}
    </ol>
  );
}
