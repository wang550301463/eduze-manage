import { Button } from '@/components/ui/Button';
import type { RosterItem } from '@/features/attendance/types';
import { rosterVisualStatus } from '@/features/attendance/types';

type Props = {
  item: RosterItem;
  onCheckIn: (item: RosterItem) => void;
  onCheckOut: (item: RosterItem) => void;
  className?: string;
};

export function CheckInButton({ item, onCheckIn, onCheckOut, className }: Props): JSX.Element | null {
  const visual = rosterVisualStatus(item.status);
  if (visual === 'checked_out' || visual === 'absent' || visual === 'leave') {
    return null;
  }
  if (visual === 'checked_in') {
    return (
      <Button size="sm" variant="secondary" className={className} onClick={() => onCheckOut(item)}>
        离园
      </Button>
    );
  }
  return (
    <Button size="sm" className={className} onClick={() => onCheckIn(item)}>
      签到
    </Button>
  );
}
