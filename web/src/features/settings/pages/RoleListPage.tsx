import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ColumnDef } from '@tanstack/react-table';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { DialogFrame } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { createRole, deleteRole, listRoles, updateRole } from '@/features/settings/api';
import type { Role } from '@/features/settings/types';
import { toast } from '@/lib/toast';

export function RoleListPage(): JSX.Element {
  const qc = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Role | null>(null);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['settings-roles'],
    queryFn: listRoles,
  });

  const saveMutation = useMutation({
    mutationFn: async () => {
      const body = { code, name };
      if (editing) return updateRole(editing.id, body);
      return createRole(body);
    },
    onSuccess: () => {
      toast.success(editing ? '已更新' : '已创建');
      setDialogOpen(false);
      void qc.invalidateQueries({ queryKey: ['settings-roles'] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteRole(id),
    onSuccess: () => {
      toast.success('已删除');
      void qc.invalidateQueries({ queryKey: ['settings-roles'] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const columns: ColumnDef<Role>[] = [
    { accessorKey: 'code', header: '编码' },
    { accessorKey: 'name', header: '名称' },
    {
      accessorKey: 'isBuiltin',
      header: '类型',
      cell: ({ row }) => (row.original.isBuiltin === 1 ? '内置' : '自定义'),
    },
    {
      id: 'actions',
      header: '操作',
      cell: ({ row }) => {
        const builtin = row.original.isBuiltin === 1;
        return (
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="ghost"
              onClick={() => {
                setEditing(row.original);
                setCode(row.original.code);
                setName(row.original.name);
                setDialogOpen(true);
              }}
            >
              编辑
            </Button>
            <Button
              size="sm"
              variant="danger"
              disabled={builtin}
              onClick={() => {
                if (window.confirm('确认删除该角色？')) deleteMutation.mutate(row.original.id);
              }}
            >
              删除
            </Button>
          </div>
        );
      },
    },
  ];

  return (
    <div className="space-y-6" data-testid="role-list-page">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">角色</h1>
        <Button
          onClick={() => {
            setEditing(null);
            setCode('');
            setName('');
            setDialogOpen(true);
          }}
        >
          新建角色
        </Button>
      </div>
      <div className="overflow-hidden rounded-xl border border-border bg-white">
        <DataTable
          columns={columns}
          data={data ?? []}
          loading={isLoading}
          rowKey={(r) => String(r.id)}
        />
      </div>
      <DialogFrame
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        title={editing ? '编辑角色' : '新建角色'}
        footer={
          <Button onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
            保存
          </Button>
        }
      >
        <div className="space-y-3">
          <div>
            <Label htmlFor="role-code">编码</Label>
            <Input
              id="role-code"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              className="mt-1"
              disabled={editing?.isBuiltin === 1}
            />
          </div>
          <div>
            <Label htmlFor="role-name">名称</Label>
            <Input id="role-name" value={name} onChange={(e) => setName(e.target.value)} className="mt-1" />
          </div>
        </div>
      </DialogFrame>
    </div>
  );
}
