import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ColumnDef } from '@tanstack/react-table';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Checkbox } from '@/components/ui/Checkbox';
import { DataTable } from '@/components/ui/DataTable';
import { DialogFrame } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { SimpleSelect } from '@/components/ui/Select';
import {
  assignUserBranches,
  assignUserRoles,
  createUser,
  deleteUser,
  listBranches,
  listRoles,
  listUsers,
  updateUser,
} from '@/features/settings/api';
import type { UserAccount } from '@/features/settings/types';
import { toast } from '@/lib/toast';

export function UserListPage(): JSX.Element {
  const qc = useQueryClient();
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<UserAccount | null>(null);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [status, setStatus] = useState('1');
  const [roleIds, setRoleIds] = useState<number[]>([]);
  const [branchIds, setBranchIds] = useState<number[]>([]);

  const { data, isLoading } = useQuery({
    queryKey: ['settings-users', page, keyword],
    queryFn: () => listUsers(page, 20, keyword || undefined),
  });

  const { data: roles = [] } = useQuery({
    queryKey: ['settings-roles'],
    queryFn: listRoles,
  });

  const { data: branches = [] } = useQuery({
    queryKey: ['settings-branches'],
    queryFn: listBranches,
  });

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (editing) {
        await updateUser(editing.id, {
          name,
          phone: phone || undefined,
          email: email || undefined,
          status: Number(status),
        });
        await assignUserRoles(editing.id, roleIds);
        await assignUserBranches(editing.id, branchIds);
        return;
      }
      const created = await createUser({
        username,
        password,
        name,
        phone: phone || undefined,
        email: email || undefined,
      });
      if (roleIds.length) await assignUserRoles(created.id, roleIds);
      if (branchIds.length) await assignUserBranches(created.id, branchIds);
    },
    onSuccess: () => {
      toast.success(editing ? '已更新' : '已创建');
      setDialogOpen(false);
      void qc.invalidateQueries({ queryKey: ['settings-users'] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteUser(id),
    onSuccess: () => {
      toast.success('已删除');
      void qc.invalidateQueries({ queryKey: ['settings-users'] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const openCreate = () => {
    setEditing(null);
    setUsername('');
    setPassword('');
    setName('');
    setPhone('');
    setEmail('');
    setStatus('1');
    setRoleIds([]);
    setBranchIds([]);
    setDialogOpen(true);
  };

  const openEdit = (row: UserAccount) => {
    setEditing(row);
    setUsername(row.username);
    setPassword('');
    setName(row.name);
    setPhone(row.phone ?? '');
    setEmail(row.email ?? '');
    setStatus(String(row.status ?? 1));
    const matchedRoleIds = roles.filter((r) => row.roles?.includes(r.code)).map((r) => r.id);
    setRoleIds(matchedRoleIds);
    setBranchIds(row.branchIds ?? []);
    setDialogOpen(true);
  };

  const toggleId = (ids: number[], id: number): number[] =>
    ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id];

  const roleNameByCode = Object.fromEntries(roles.map((r) => [r.code, r.name]));

  const columns: ColumnDef<UserAccount>[] = [
    { accessorKey: 'username', header: '登录名' },
    { accessorKey: 'name', header: '姓名' },
    { accessorKey: 'phone', header: '手机' },
    {
      accessorKey: 'roles',
      header: '角色',
      cell: ({ row }) =>
        row.original.roles?.map((c) => roleNameByCode[c] ?? c).join('、') || '-',
    },
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
              if (window.confirm('确认删除该账号？')) deleteMutation.mutate(row.original.id);
            }}
          >
            删除
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-4" data-testid="user-list-page">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="font-serif text-xl font-semibold">账号</h1>
        <div className="flex flex-wrap gap-2">
          <Input
            placeholder="搜索登录名 / 姓名"
            value={keyword}
            onChange={(e) => {
              setKeyword(e.target.value);
              setPage(1);
            }}
            className="max-w-xs"
          />
          <Button onClick={openCreate}>新建账号</Button>
        </div>
      </div>
      <DataTable
        columns={columns}
        data={data?.items ?? []}
        loading={isLoading}
        rowKey={(r) => String(r.id)}
        pageState={{
          page,
          pageSize: 20,
          total: data?.total ?? 0,
          onChange: ({ page: p }) => setPage(p),
        }}
      />
      <DialogFrame
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        title={editing ? '编辑账号' : '新建账号'}
        footer={
          <Button onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
            保存
          </Button>
        }
      >
        <div className="space-y-3">
          {!editing ? (
            <>
              <div>
                <Label htmlFor="user-username">登录名</Label>
                <Input
                  id="user-username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="mt-1"
                />
              </div>
              <div>
                <Label htmlFor="user-password">密码</Label>
                <Input
                  id="user-password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="mt-1"
                />
              </div>
            </>
          ) : null}
          <div>
            <Label htmlFor="user-name">姓名</Label>
            <Input id="user-name" value={name} onChange={(e) => setName(e.target.value)} className="mt-1" />
          </div>
          <div>
            <Label htmlFor="user-phone">手机</Label>
            <Input id="user-phone" value={phone} onChange={(e) => setPhone(e.target.value)} className="mt-1" />
          </div>
          <div>
            <Label htmlFor="user-email">邮箱</Label>
            <Input id="user-email" value={email} onChange={(e) => setEmail(e.target.value)} className="mt-1" />
          </div>
          {editing ? (
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
          ) : null}
          <div>
            <Label>角色</Label>
            <div className="mt-2 flex flex-wrap gap-3">
              {roles.map((r) => (
                <label key={r.id} className="flex items-center gap-2 text-sm">
                  <Checkbox
                    checked={roleIds.includes(r.id)}
                    onCheckedChange={() => setRoleIds((ids) => toggleId(ids, r.id))}
                  />
                  {r.name}
                </label>
              ))}
            </div>
          </div>
          <div>
            <Label>校区</Label>
            <div className="mt-2 flex flex-wrap gap-3">
              {branches.map((b) => (
                <label key={b.id} className="flex items-center gap-2 text-sm">
                  <Checkbox
                    checked={branchIds.includes(b.id)}
                    onCheckedChange={() => setBranchIds((ids) => toggleId(ids, b.id))}
                  />
                  {b.name}
                </label>
              ))}
            </div>
          </div>
        </div>
      </DialogFrame>
    </div>
  );
}
