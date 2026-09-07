const DOW_LABELS = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'];

export function minutesToHHMM(min: number): string {
  const h = Math.floor(min / 60);
  const m = min % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

export function hhmmToMinutes(s: string): number {
  const [h, m] = s.split(':').map((p) => Number(p));
  return h * 60 + m;
}

export function formatDayOfWeek(dayOfWeek?: number | null): string {
  if (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7) return '—';
  return DOW_LABELS[dayOfWeek] ?? '—';
}

/** 周几 + 起止时间，如「周六 09:00-10:30」 */
export function formatScheduleSlot(
  dayOfWeek?: number | null,
  startMinute?: number | null,
  endMinute?: number | null,
): string {
  if (dayOfWeek == null || startMinute == null || endMinute == null) return '—';
  return `${formatDayOfWeek(dayOfWeek)} ${minutesToHHMM(startMinute)}-${minutesToHHMM(endMinute)}`;
}

export const DAY_OF_WEEK_OPTIONS = [
  { value: '1', label: '周一' },
  { value: '2', label: '周二' },
  { value: '3', label: '周三' },
  { value: '4', label: '周四' },
  { value: '5', label: '周五' },
  { value: '6', label: '周六' },
  { value: '7', label: '周日' },
];
