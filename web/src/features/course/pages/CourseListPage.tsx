import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ColumnDef } from '@tanstack/react-table';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { DialogFrame } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { createCourse, deleteCourse, listCourses, updateCourse } from '@/features/course/api';
import type { Course } from '@/features/course/types';
import { toast } from '@/lib/toast';

export function CourseListPage(): JSX.Element {
  const qc = useQueryClient();
  const [page, setPage] = useState(1);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Course | null>(null);
  const [name, setName] = useState('');
  const [lessonMinutes, setLessonMinutes] = useState('90');

  const { data, isLoading } = useQuery({
    queryKey: ['courses', page],
    queryFn: () => listCourses(page, 20),
  });

  const saveMutation = useMutation({
    mutationFn: async () => {
      const body = { name, lessonMinutes: Number(lessonMinutes) || undefined };
      if (editing) return updateCourse(editing.id, body);
      return createCourse(body);
    },
    onSuccess: () => {
      toast.success(editing ? '已更新' : '已创建');
      setDialogOpen(false);
      void qc.invalidateQueries({ queryKey: ['courses'] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteCourse(id),
    onSuccess: () => {
      toast.success('已删除');
      void qc.invalidateQueries({ queryKey: ['courses'] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const columns: ColumnDef<Course>[] = [
    { accessorKey: 'name', header: '课程名称' },
    { accessorKey: 'lessonMinutes', header: '课时长(分)' },
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
              setLessonMinutes(String(row.original.lessonMinutes ?? 90));
              setDialogOpen(true);
            }}
          >
            编辑
          </Button>
          <Button
            size="sm"
            variant="danger"
            onClick={() => {
              if (window.confirm('确认删除？')) deleteMutation.mutate(row.original.id);
            }}
          >
            删除
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-4" data-testid="course-list-page">
      <div className="flex items-center justify-between">
        <h1 className="font-serif text-xl font-semibold">课程产品</h1>
        <Button
          onClick={() => {
            setEditing(null);
            setName('');
            setLessonMinutes('90');
            setDialogOpen(true);
          }}
        >
          新建课程
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
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        title={editing ? '编辑课程' : '新建课程'}
        footer={
          <Button onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
            保存
          </Button>
        }
      >
        <div className="space-y-3">
          <div>
            <Label htmlFor="course-name">名称</Label>
            <Input id="course-name" value={name} onChange={(e) => setName(e.target.value)} className="mt-1" />
          </div>
          <div>
            <Label htmlFor="course-minutes">课时长(分钟)</Label>
            <Input
              id="course-minutes"
              type="number"
              value={lessonMinutes}
              onChange={(e) => setLessonMinutes(e.target.value)}
              className="mt-1"
            />
          </div>
        </div>
      </DialogFrame>
    </div>
  );
}
