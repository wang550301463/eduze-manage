import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ColumnDef } from '@tanstack/react-table';
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { SheetFrame } from '@/components/ui/Sheet';
import {
  getClassGroup,
  listClassGroups,
  listClassMembers,
  removeClassMember,
} from '@/features/course/api';
import { ClassGroupFormDialog } from '@/features/course/components/ClassGroupFormDialog';
import { useDefaultBranchId } from '@/features/course/hooks/useDefaultBranchId';
import type { ClassGroup } from '@/features/course/types';
import { toast } from '@/lib/toast';

export function ClassGroupListPage(): JSX.Element {
  const branchId = useDefaultBranchId();
  const qc = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const openId = searchParams.get('openId');
  const [page, setPage] = useState(1);
  const [formOpen, setFormOpen] = useState(false);
  const [detailId, setDetailId] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['class-groups', page, branchId],
    queryFn: () => listClassGroups(page, 20, branchId),
  });

  const detailQuery = useQuery({
    queryKey: ['class-group', detailId],
    queryFn: () => getClassGroup(detailId!),
    enabled: detailId != null,
  });

  const membersQuery = useQuery({
    queryKey: ['class-group-members', detailId],
    queryFn: () => listClassMembers(detailId!),
    enabled: detailId != null,
  });

  useEffect(() => {
    if (openId) setDetailId(openId);
  }, [openId]);

  const removeMutation = useMutation({
    mutationFn: ({ groupId, studentId }: { groupId: string | number; studentId: string | number }) =>
      removeClassMember(groupId, studentId),
    onSuccess: () => {
      toast.success('已转出');
      void qc.invalidateQueries({ queryKey: ['class-group-members'] });
      void qc.invalidateQueries({ queryKey: ['class-groups'] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const columns: ColumnDef<ClassGroup>[] = [
    { accessorKey: 'name', header: '班级' },
    { accessorKey: 'courseName', header: '课程' },
    {
      id: 'capacity',
      header: '人数',
      cell: ({ row }) => `${row.original.currentCount}/${row.original.capacity}`,
    },
    { accessorKey: 'headTeacherName', header: '班主任' },
  ];

  const closeDetail = () => {
    setDetailId(null);
    const next = new URLSearchParams(searchParams);
    next.delete('openId');
    setSearchParams(next, { replace: true });
  };

  const group = detailQuery.data;
  const overCapacity = group ? group.currentCount > group.capacity : false;

  return (
    <div className="space-y-4" data-testid="class-group-list-page">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="font-serif text-xl font-semibold">分组标签（旧称：班级）</h1>
        <Button onClick={() => setFormOpen(true)}>新建分组</Button>
      </div>
      <div className="rounded-md border border-warning/40 bg-warning/10 p-3 text-sm text-warning">
        班级已降级为「分组标签」，用于花名册分类。排课请前往「按老师周课表」基于老师可用时段配置。
      </div>
      <DataTable
        columns={columns}
        data={data?.items ?? []}
        loading={isLoading}
        rowKey={(r) => String(r.id)}
        onRowClick={(row) => setDetailId(String(row.id))}
        pageState={{
          page,
          pageSize: 20,
          total: data?.total ?? 0,
          onChange: ({ page: p }) => setPage(p),
        }}
      />
      <ClassGroupFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        branchId={branchId}
        onSaved={() => void qc.invalidateQueries({ queryKey: ['class-groups'] })}
      />
      <SheetFrame
        open={detailId != null}
        onOpenChange={(open) => {
          if (!open) closeDetail();
        }}
        title={group?.name ?? '班级详情'}
        footer={null}
      >
        {group ? (
          <div className="space-y-4 text-sm">
            <p>
              课程：{group.courseName} · 班主任：{group.headTeacherName ?? '—'}
            </p>
            <div>
              <div className="mb-1 flex justify-between">
                <span>容量</span>
                <span className={overCapacity ? 'text-danger' : ''}>
                  {group.currentCount}/{group.capacity}
                </span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-muted">
                <div
                  className={`h-full ${overCapacity ? 'bg-danger' : 'bg-primary'}`}
                  style={{ width: `${Math.min(100, (group.currentCount / group.capacity) * 100)}%` }}
                />
              </div>
            </div>
            <div>
              <h3 className="mb-2 font-medium">成员</h3>
              <ul className="space-y-2">
                {(membersQuery.data ?? []).map((m) => (
                  <li key={m.studentId} className="flex items-center justify-between">
                    <span>
                      {m.studentName} ({m.enrollNo})
                    </span>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() =>
                        removeMutation.mutate({ groupId: group.id, studentId: m.studentId })
                      }
                    >
                      转出
                    </Button>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        ) : null}
      </SheetFrame>
    </div>
  );
}
