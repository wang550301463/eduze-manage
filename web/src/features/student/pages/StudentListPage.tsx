import { useQuery } from '@tanstack/react-query';
import type { ColumnDef } from '@tanstack/react-table';
import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Checkbox } from '@/components/ui/Checkbox';
import { DataTable } from '@/components/ui/DataTable';
import { Input } from '@/components/ui/Input';
import { useUrlState } from '@/hooks/useUrlState';
import { cn } from '@/lib/cn';
import { branchApi, studentApi } from '../api';
import { BulkActionBar } from '../components/BulkActionBar';
import { BulkAssignClassDialog } from '../components/BulkAssignClassDialog';
import { StudentDetailSheet } from '../components/StudentDetailSheet';
import { StudentFilterBar } from '../components/StudentFilterBar';
import { StudentFormDialog } from '../components/StudentFormDialog';
import type { EntityId, Student } from '../types';
import { calcAge, genderLabel, statusBadgeClass, statusLabel } from '../utils';

export function StudentListPage(): JSX.Element {
  const [searchParams, setSearchParams] = useSearchParams();
  const [filters, setFilters] = useUrlState({
    keyword: '',
    branchId: '',
    status: '',
    lowBalance: '',
    page: '1',
    size: '20',
  });
  const [selected, setSelected] = useState<Set<EntityId>>(new Set());
  const [detailId, setDetailId] = useState<EntityId | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [bulkMode, setBulkMode] = useState<'assign' | 'transfer' | null>(null);

  const openFromUrl = searchParams.get('openId');
  useEffect(() => {
    if (openFromUrl) setDetailId(openFromUrl);
  }, [openFromUrl]);

  const openDetail = (id: EntityId) => {
    setDetailId(String(id));
    const next = new URLSearchParams(searchParams);
    next.set('openId', String(id));
    setSearchParams(next, { replace: true });
  };

  const closeDetail = () => {
    setDetailId(null);
    const next = new URLSearchParams(searchParams);
    next.delete('openId');
    next.delete('openGuardianId');
    setSearchParams(next, { replace: true });
  };

  const { data: branchesData } = useQuery({
    queryKey: ['branches'],
    queryFn: () => branchApi.list(),
  });
  const branches = branchesData ?? [];

  const listParams = useMemo(
    () => ({
      keyword: filters.keyword || undefined,
      branchId: filters.branchId || undefined,
      status: filters.status || undefined,
      pkgRemainingMax: filters.lowBalance || undefined,
      page: filters.page,
      size: filters.size,
    }),
    [filters],
  );

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['students', listParams],
    queryFn: () => studentApi.list(listParams),
  });

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'n') {
        e.preventDefault();
        setFormOpen(true);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  const columns: ColumnDef<Student>[] = [
    {
      id: 'select',
      header: '',
      cell: ({ row }) => (
        <Checkbox
          checked={selected.has(row.original.id)}
          onCheckedChange={(c) => {
            setSelected((prev) => {
              const next = new Set(prev);
              if (c === true) next.add(row.original.id);
              else next.delete(row.original.id);
              return next;
            });
          }}
          aria-label="选择行"
          onClick={(e) => e.stopPropagation()}
        />
      ),
    },
    { accessorKey: 'name', header: '姓名' },
    { accessorKey: 'enrollNo', header: '入园编号' },
    {
      accessorKey: 'gender',
      header: '性别',
      cell: ({ row }) => genderLabel(row.original.gender),
    },
    {
      id: 'age',
      header: '年龄',
      cell: ({ row }) => calcAge(row.original.birthday),
    },
    {
      id: 'mentor',
      header: '主带老师',
      cell: ({ row }) => row.original.mentorTeacherName ?? '-',
    },
    {
      id: 'stage',
      header: '阶段',
      cell: ({ row }) => row.original.currentStageName ?? '-',
    },
    {
      id: 'class',
      header: '分组',
      cell: ({ row }) =>
        row.original.classGroups.length
          ? row.original.classGroups.map((c) => c.name).join('、')
          : '-',
    },
    {
      id: 'balance',
      header: '课时余额',
      cell: ({ row }) => (
        <span className={cn(row.original.alertLow && 'font-medium text-danger')}>
          {row.original.totalRemaining}
        </span>
      ),
    },
    {
      id: 'status',
      header: '状态',
      cell: ({ row }) => (
        <span
          className={cn(
            'rounded-full px-2 py-0.5 text-xs',
            statusBadgeClass(row.original.status),
          )}
        >
          {statusLabel(row.original.status)}
        </span>
      ),
    },
    {
      id: 'actions',
      header: '操作',
      cell: ({ row }) => (
        <Button
          size="sm"
          variant="ghost"
          onClick={(e) => {
            e.stopPropagation();
            openDetail(row.original.id);
          }}
        >
          详情
        </Button>
      ),
    },
  ];

  const page = Number(filters.page) || 1;

  return (
    <div className="space-y-4 pb-24" data-testid="student-list-page">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-2xl font-semibold tracking-tight">学员</h1>
        <div className="flex flex-wrap gap-2">
          <Input
            placeholder="搜索姓名 / 入园编号"
            value={filters.keyword}
            onChange={(e) => setFilters({ keyword: e.target.value, page: '1' })}
            className="max-w-xs"
          />
          <Button onClick={() => setFormOpen(true)}>新建学员</Button>
          <a
            href="/api/students/import/template"
            className="inline-flex h-10 items-center rounded-md border border-border px-4 text-sm hover:bg-muted"
          >
            批量导入
          </a>
        </div>
      </div>
      <StudentFilterBar
        filters={filters}
        branches={branches}
        onChange={(p) => setFilters({ ...p, page: '1' })}
        onReset={() =>
          setFilters({ keyword: '', branchId: '', status: '', lowBalance: '', page: '1' })
        }
      />
      <div className="overflow-hidden rounded-xl border border-border bg-white">
        <DataTable
          columns={columns}
          data={data?.records ?? []}
          loading={isLoading}
          rowKey={(r) => String(r.id)}
          onRowClick={(row) => openDetail(row.id)}
          pageState={{
            page,
            pageSize: Number(filters.size) || 20,
            total: data?.total ?? 0,
            onChange: ({ page: p }) => setFilters({ page: String(p) }),
          }}
          mobileCardRender={(row) => (
            <>
              <div className="flex items-center justify-between">
                <span className="font-medium">{row.name}</span>
                <span
                  className={cn(
                    'rounded-full px-2 py-0.5 text-xs',
                    statusBadgeClass(row.status),
                  )}
                >
                  {statusLabel(row.status)}
                </span>
              </div>
              <p className="text-sm text-muted-fg">
                {row.classGroups.map((c) => c.name).join('、') || '未分班'} ·{' '}
                <span className={cn(row.alertLow && 'text-danger')}>课时 {row.totalRemaining}</span>
              </p>
            </>
          )}
        />
      </div>
      <BulkActionBar
        count={selected.size}
        onAssign={() => setBulkMode('assign')}
        onTransfer={() => setBulkMode('transfer')}
        onClear={() => setSelected(new Set())}
      />
      <StudentDetailSheet
        studentId={detailId}
        guardianId={searchParams.get('openGuardianId')}
        open={detailId != null}
        onOpenChange={(o) => !o && closeDetail()}
        branches={branches}
      />
      <StudentFormDialog open={formOpen} onOpenChange={setFormOpen} branches={branches} />
      <BulkAssignClassDialog
        open={bulkMode != null}
        onOpenChange={(o) => !o && setBulkMode(null)}
        studentIds={[...selected]}
        mode={bulkMode ?? 'assign'}
        branchId={filters.branchId || undefined}
        onDone={() => {
          setSelected(new Set());
          void refetch();
        }}
      />
    </div>
  );
}
