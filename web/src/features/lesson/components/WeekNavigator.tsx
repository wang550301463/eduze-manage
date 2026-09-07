import { addDays, format, startOfWeek } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import { Button } from '@/components/ui/Button';

type Props = {
  weekStart: Date;
  onChange: (start: Date) => void;
};

export function WeekNavigator({ weekStart, onChange }: Props): JSX.Element {
  const end = addDays(weekStart, 6);
  const go = (delta: number) => onChange(addDays(weekStart, delta * 7));
  const thisWeek = () => onChange(startOfWeek(new Date(), { weekStartsOn: 1 }));

  return (
    <div className="inline-flex flex-wrap items-center gap-2">
      <div className="inline-flex rounded-lg bg-muted p-1">
        <Button variant="ghost" size="sm" className="h-8 px-2" onClick={() => go(-1)}>
          ‹
        </Button>
        <button
          type="button"
          className="rounded-md bg-white px-3 py-1.5 text-sm font-medium text-foreground shadow-sm"
          onClick={thisWeek}
        >
          本周
        </button>
        <Button variant="ghost" size="sm" className="h-8 px-2" onClick={() => go(1)}>
          ›
        </Button>
      </div>
      <span className="text-sm text-muted-fg">
        {format(weekStart, 'M月d日', { locale: zhCN })} – {format(end, 'M月d日', { locale: zhCN })}
      </span>
    </div>
  );
}
