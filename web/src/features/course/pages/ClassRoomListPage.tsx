import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ColumnDef } from '@tanstack/react-table';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { DialogFrame } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import {
  createClassRoom,
  deleteClassRoom,
  listClassRooms,
  updateClassRoom,
} from '@/features/course/api';
import { useDefaultBranchId } from '@/features/course/hooks/useDefaultBranchId';
import type { ClassRoom } from '@/features/course/types';
import { toast } from '@/lib/toast';

export function ClassRoomListPage(): JSX.Element {
  const branchId = useDefaultBranchId();
  const qc = useQueryClient();
  const [page, setPage] = useState(1);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<ClassRoom | null>(null);
  const [name, setName] = useState('');
  const [capacity, setCapacity] = useState('12');

  const { data, isLoading } = useQuery({
    queryKey: ['class-rooms', page, branchId],
    queryFn: () => listClassRooms(page, 20, branchId),
  });

  const saveMutation = useMutation({
    mutationFn: async () => {
      const body = { branchId, name, capacity: Number(capacity) || undefined };
      if (editing) return updateClassRoom(editing.id, body);
      return createClassRoom(body);
    },
    onSuccess: () => {
      toast.success('已保存');
      setOpen(false);
      void qc.invalidateQueries({ queryKey: ['class-rooms'] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const columns: ColumnDef<ClassRoom>[] = [
    { accessorKey: 'name', header: '画室' },
    { accessorKey: 'capacity', header: '容量' },
    {
      id: 'actions',
      header: '操作',
      cell: ({ row }) => (
        <div className="flex gap-2">
          <Button
            size="sm"
            variant="ghost"
            onClick={() => {
              setEditing(row.original);
              setName(row.original.name);
              setCapacity(String(row.original.capacity ?? 12));
              setOpen(true);
            }}
          >
            编辑
          </Button>
          <Button
            size="sm"
            variant="danger"
            onClick={() => {
              if (window.confirm('确认删除？')) {
                deleteClassRoom(row.original.id)
                  .then(() => {
                    toast.success('已删除');
                    void qc.invalidateQueries({ queryKey: ['class-rooms'] });
                  })
                  .catch((e: Error) => toast.error(e.message));
              }
            }}
          >
            删除
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-4" data-testid="class-room-list-page">
      <div className="flex items-center justify-between">
        <h1 className="font-serif text-xl font-semibold">画室</h1>
        <Button
          onClick={() => {
            setEditing(null);
            setName('');
            setCapacity('12');
            setOpen(true);
          }}
        >
          新建画室
        </Button>
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
        open={open}
        onOpenChange={setOpen}
        title={editing ? '编辑画室' : '新建画室'}
        footer={<Button onClick={() => saveMutation.mutate()}>保存</Button>}
      >
        <div className="space-y-3">
          <div>
            <Label>名称</Label>
            <Input value={name} onChange={(e) => setName(e.target.value)} className="mt-1" />
          </div>
          <div>
            <Label>容量</Label>
            <Input type="number" value={capacity} onChange={(e) => setCapacity(e.target.value)} className="mt-1" />
          </div>
        </div>
      </DialogFrame>
    </div>
  );
}
