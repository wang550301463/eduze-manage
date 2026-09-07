import type { ColumnDef } from '@tanstack/react-table';
import { format, parseISO } from 'date-fns';
import { DataTable } from '@/components/ui/DataTable';
import { AttendanceStatusBadge } from '@/features/attendance/components/AttendanceStatusBadge';
import { CheckInButton } from '@/features/attendance/components/CheckInButton';
import type { RosterItem } from '@/features/attendance/types';
import { rosterVisualStatus } from '@/features/attendance/types';

type Props = {
  items: RosterItem[];
  readOnly?: boolean;
  onCheckIn: (item: RosterItem) => void;
  onCheckOut: (item: RosterItem) => void;
};

export function AttendanceRosterTable({
  items,
  readOnly = false,
  onCheckIn,
  onCheckOut,
}: Props): JSX.Element {
  const columns: ColumnDef<RosterItem>[] = [
    {
      accessorKey: 'studentName',
      header: '学员',
      cell: ({ row }) => (
        <div>
          <p className="font-medium">{row.original.studentName}</p>
          <p className="text-xs text-muted-fg">{row.original.classGroupName}</p>
        </div>
      ),
    },
    {
      id: 'time',
      header: '时段',
      cell: ({ row }) => format(parseISO(row.original.lessonStartAt), 'HH:mm'),
    },
    {
      id: 'status',
      header: '状态',
      cell: ({ row }) => (
        <AttendanceStatusBadge
          status={rosterVisualStatus(row.original.status)}
          label={row.original.statusLabel}
        />
      ),
    },
    ...(readOnly
      ? []
      : [
          {
            id: 'actions',
            header: '',
            cell: ({ row }: { row: { original: RosterItem } }) => (
              <CheckInButton
                item={row.original}
                onCheckIn={onCheckIn}
                onCheckOut={onCheckOut}
              />
            ),
          } as ColumnDef<RosterItem>,
        ]),
  ];

  return (
    <DataTable
      columns={columns}
      data={items}
      rowKey={(r) => `${r.lessonId}-${r.studentId}`}
      mobileCardRender={(row) => (
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="font-medium">{row.studentName}</p>
            <p className="text-xs text-muted-fg">
              {row.classGroupName} · {format(parseISO(row.lessonStartAt), 'HH:mm')}
            </p>
            <div className="mt-2">
              <AttendanceStatusBadge
                status={rosterVisualStatus(row.status)}
                label={row.statusLabel}
              />
            </div>
          </div>
          {!readOnly ? (
            <CheckInButton
              item={row}
              className="w-full min-w-[5rem] sm:w-auto"
              onCheckIn={onCheckIn}
              onCheckOut={onCheckOut}
            />
          ) : null}
        </div>
      )}
    />
  );
}
