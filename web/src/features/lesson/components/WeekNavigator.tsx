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
    <div className="flex flex-wrap items-center gap-2">
      <Button variant="ghost" size="sm" onClick={() => go(-1)}>
        ‹
      </Button>
      <Button variant="secondary" size="sm" onClick={thisWeek}>
        本周
      </Button>
      <Button variant="ghost" size="sm" onClick={() => go(1)}>
        ›
      </Button>
      <span className="text-sm text-muted-fg">
        {format(weekStart, 'M月d日', { locale: zhCN })} – {format(end, 'M月d日', { locale: zhCN })}
      </span>
    </div>
  );
}
