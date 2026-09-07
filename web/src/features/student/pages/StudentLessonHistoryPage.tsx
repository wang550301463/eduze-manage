import { useQuery } from '@tanstack/react-query';
import type { ColumnDef } from '@tanstack/react-table';
import { useMemo, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { Input } from '@/components/ui/Input';
import { useDefaultBranchId } from '@/features/course/hooks/useDefaultBranchId';
import { studentApi } from '../api';
import type { StudentLessonHistory } from '../types';

function formatDateTime(iso?: string | null): string {
  if (!iso) return '—';
  try {
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return iso;
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hh = String(d.getHours()).padStart(2, '0');
    const mm = String(d.getMinutes()).padStart(2, '0');
    return `${y}-${m}-${day} ${hh}:${mm}`;
  } catch {
    return iso;
  }
}

function attendanceStatusLabel(status?: number | null): string {
  if (status == null) return '—';
  switch (status) {
    case 2:
      return '已入园';
    case 3:
      return '已离园';
    case 4:
      return '缺勤';
    case 5:
      return '请假';
    default:
      return String(status);
  }
}

export function StudentLessonHistoryPage(): JSX.Element {
  const branchId = useDefaultBranchId();
  const [studentId, setStudentId] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(1);
  const [applied, setApplied] = useState({ studentId: '', from: '', to: '' });

  const listParams = useMemo(
    () => ({
      studentId: applied.studentId || undefined,
      branchId: String(branchId),
      from: applied.from || undefined,
      to: applied.to || undefined,
      page: String(page),
      size: '20',
    }),
    [applied, branchId, page],
  );

  const { data, isLoading } = useQuery({
    queryKey: ['student-lesson-histories', listParams],
    queryFn: () => studentApi.listLessonHistories(listParams),
  });

  const columns: ColumnDef<StudentLessonHistory>[] = [
    {
      id: 'time',
      header: '时间',
      cell: ({ row }) => formatDateTime(row.original.startAt ?? row.original.occurredAt),
    },
    { accessorKey: 'studentName', header: '学员' },
    {
      id: 'courseGroup',
      header: '课程/分组',
      cell: ({ row }) => {
        const course = row.original.courseName ?? '—';
        const group = row.original.classGroupName;
        return group ? `${course} / ${group}` : course;
      },
    },
    {
      id: 'teacher',
      header: '老师',
      cell: ({ row }) => row.original.teacherName ?? '—',
    },
    {
      id: 'source',
      header: '类型',
      cell: ({ row }) => row.original.sourceLabel ?? '—',
    },
    {
      id: 'attendance',
      header: '出勤状态',
      cell: ({ row }) => attendanceStatusLabel(row.original.attendanceStatus),
    },
    {
      id: 'minutes',
      header: '时长',
      cell: ({ row }) =>
        row.original.minutes != null ? `${row.original.minutes} 分钟` : '—',
    },
  ];

  const onSearch = () => {
    setPage(1);
    setApplied({ studentId: studentId.trim(), from, to });
  };

  return (
    <div className="space-y-4" data-testid="student-lesson-history-page">
      <h1 className="font-serif text-xl font-semibold tracking-tight">课程记录</h1>
      <div className="rounded-md border border-border bg-background p-4">
        <div className="flex flex-wrap items-end gap-3">
          <label className="space-y-1 text-sm">
            <span className="text-muted-fg">学员 ID</span>
            <Input
              value={studentId}
              onChange={(e) => setStudentId(e.target.value)}
              placeholder="可选"
              className="w-40"
            />
          </label>
          <label className="space-y-1 text-sm">
            <span className="text-muted-fg">开始日期</span>
            <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="w-40" />
          </label>
          <label className="space-y-1 text-sm">
            <span className="text-muted-fg">结束日期</span>
            <Input type="date" value={to} onChange={(e) => setTo(e.target.value)} className="w-40" />
          </label>
          <Button type="button" onClick={onSearch}>
            查询
          </Button>
        </div>
      </div>
      <div className="rounded-md border border-border bg-background">
        <DataTable
          columns={columns}
          data={data?.records ?? []}
          loading={isLoading}
          rowKey={(r) => String(r.id)}
          pageState={{
            page,
            pageSize: 20,
            total: data?.total ?? 0,
            onChange: ({ page: p }) => setPage(p),
          }}
        />
      </div>
    </div>
  );
}
