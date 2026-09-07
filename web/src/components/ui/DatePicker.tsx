import { zhCN } from 'date-fns/locale';
import { useState } from 'react';
import { DayPicker } from 'react-day-picker';
import { cn } from '@/lib/cn';
import 'react-day-picker/style.css';

export type DatePickerProps = {
  value?: Date;
  onChange?: (date: Date | undefined) => void;
  className?: string;
};

export function DatePicker({ value, onChange, className }: DatePickerProps): JSX.Element {
  const [selected, setSelected] = useState<Date | undefined>(value);

  const handleSelect = (date: Date | undefined): void => {
    setSelected(date);
    onChange?.(date);
  };

  return (
    <div
      className={cn(
        'rounded-lg border border-border bg-background p-3 shadow-sm',
        className,
      )}
    >
      <DayPicker
        mode="single"
        selected={value ?? selected}
        onSelect={handleSelect}
        locale={zhCN}
        classNames={{
          root: 'text-sm',
          day_button: 'rounded-md hover:bg-muted',
          selected: 'bg-primary text-primary-fg rounded-md',
          today: 'font-semibold text-primary',
        }}
      />
    </div>
  );
}
