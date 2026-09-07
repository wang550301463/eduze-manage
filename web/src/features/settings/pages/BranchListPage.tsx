import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ColumnDef } from '@tanstack/react-table';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { DialogFrame } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { SimpleSelect } from '@/components/ui/Select';
import {
  createBranch,
  deleteBranch,
  listBranches,
  updateBranch,
} from '@/features/settings/api';
import type { Branch } from '@/features/settings/types';
import { toast } from '@/lib/toast';

export function BranchListPage(): JSX.Element {
  const qc = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Branch | null>(null);
  const [name, setName] = useState('');
  const [code, setCode] = useState('');
  const [address, setAddress] = useState('');
  const [phone, setPhone] = useState('');
  const [status, setStatus] = useState('1');

  const { data, isLoading } = useQuery({
    queryKey: ['settings-branches'],
    queryFn: listBranches,
  });

  const saveMutation = useMutation({
    mutationFn: async () => {
      const body = {
        name,
        code,
        address: address || undefined,
        phone: phone || undefined,
        status: Number(status),
      };
      if (editing) return updateBranch(editing.id, body);
      return createBranch(body);
    },
    onSuccess: () => {
      toast.success(editing ? '已更新' : '已创建');
      setDialogOpen(false);
      void qc.invalidateQueries({ queryKey: ['settings-branches'] });
      void qc.invalidateQueries({ queryKey: ['branches'] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteBranch(id),
    onSuccess: () => {
      toast.success('已删除');
      void qc.invalidateQueries({ queryKey: ['settings-branches'] });
      void qc.invalidateQueries({ queryKey: ['branches'] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const openCreate = () => {
    setEditing(null);
    setName('');
    setCode('');
    setAddress('');
    setPhone('');
    setStatus('1');
    setDialogOpen(true);
  };

  const openEdit = (row: Branch) => {
    setEditing(row);
    setName(row.name);
    setCode(row.code);
    setAddress(row.address ?? '');
    setPhone(row.phone ?? '');
    setStatus(String(row.status ?? 1));
    setDialogOpen(true);
  };

  const columns: ColumnDef<Branch>[] = [
    { accessorKey: 'name', header: '校区名称' },
    { accessorKey: 'code', header: '编码' },
    { accessorKey: 'address', header: '地址' },
    { accessorKey: 'phone', header: '电话' },
    {
      accessorKey: 'status',
      header: '状态',
      cell: ({ row }) => (row.original.status === 1 ? '启用' : '停用'),
    },
    {
      id: 'actions',
      header: '操作',
      cell: ({ row }) => (
        <div className="flex gap-2">
          <Button size="sm" variant="ghost" onClick={() => openEdit(row.original)}>
            编辑
          </Button>
          <Button
            size="sm"
            variant="danger"
            onClick={() => {
              if (window.confirm('确认删除该校区？')) deleteMutation.mutate(row.original.id);
            }}
          >
            删除
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6" data-testid="branch-list-page">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">校区</h1>
        <Button onClick={openCreate}>新建校区</Button>
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
        title={editing ? '编辑校区' : '新建校区'}
        footer={
          <Button onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
            保存
          </Button>
        }
      >
        <div className="space-y-3">
          <div>
            <Label htmlFor="branch-name">名称</Label>
            <Input id="branch-name" value={name} onChange={(e) => setName(e.target.value)} className="mt-1" />
          </div>
          <div>
            <Label htmlFor="branch-code">编码</Label>
            <Input id="branch-code" value={code} onChange={(e) => setCode(e.target.value)} className="mt-1" />
          </div>
          <div>
            <Label htmlFor="branch-address">地址</Label>
            <Input
              id="branch-address"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              className="mt-1"
            />
          </div>
          <div>
            <Label htmlFor="branch-phone">电话</Label>
            <Input id="branch-phone" value={phone} onChange={(e) => setPhone(e.target.value)} className="mt-1" />
          </div>
          <div>
            <Label>状态</Label>
            <SimpleSelect
              className="mt-1"
              aria-label="状态"
              value={status}
              onValueChange={setStatus}
              options={[
                { value: '1', label: '启用' },
                { value: '0', label: '停用' },
              ]}
            />
          </div>
        </div>
      </DialogFrame>
    </div>
  );
}
