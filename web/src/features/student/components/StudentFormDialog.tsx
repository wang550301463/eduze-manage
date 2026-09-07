import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import type { FieldErrors } from 'react-hook-form';
import { Button } from '@/components/ui/Button';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { SimpleSelect } from '@/components/ui/Select';
import { ApiError, formatApiErrorMessage } from '@/lib/api';
import { cn } from '@/lib/cn';
import { toast } from '@/lib/toast';
import { studentApi, teacherApi } from '../api';
import { studentFormSchema, type StudentFormValues } from '../schemas';
import type { Branch, Student } from '../types';
import { GuardianSubForm } from './GuardianSubForm';
const STEPS = ['基础', '健康', '家长', '班级'];

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  branches: Branch[];
  student?: Student | null;
  onSaved?: () => void;
};

export function StudentFormDialog({
  open,
  onOpenChange,
  branches,
  student,
  onSaved,
}: Props): JSX.Element {
  const qc = useQueryClient();
  const [step, setStep] = useState(0);
  const editing = Boolean(student);

  const defaultBranchId = String(student?.branchId ?? branches[0]?.id ?? '');

  const form = useForm<StudentFormValues>({
    resolver: zodResolver(studentFormSchema),
    defaultValues: {
      branchId: defaultBranchId,
      enrollNo: student?.enrollNo ?? '',
      name: student?.name ?? '',
      gender: student?.gender ?? 0,
      birthday: student?.birthday ?? '',
      enrollDate: student?.enrollDate ?? '',
      allergy: student?.allergy ?? '',
      healthNote: student?.healthNote ?? '',
      emergencyContact: student?.emergencyContact ?? '',
      emergencyPhone: student?.emergencyPhone ?? '',
      mentorTeacherId: student?.mentorTeacherId ? String(student.mentorTeacherId) : '',
      currentStageId: student?.currentStageId ? String(student.currentStageId) : '',
      skipClass: true,
    },
  });

  const watchedBranchId = form.watch('branchId');
  const teachersQuery = useQuery({
    queryKey: ['teachers', watchedBranchId],
    queryFn: () => teacherApi.list(watchedBranchId ? watchedBranchId : undefined),
    enabled: open,
  });

  useEffect(() => {
    if (!open) return;
    setStep(0);
    const branchId = String(student?.branchId ?? branches[0]?.id ?? '');
    form.reset({
      branchId,
      enrollNo: student?.enrollNo ?? '',
      name: student?.name ?? '',
      gender: student?.gender ?? 0,
      birthday: student?.birthday ?? '',
      enrollDate: student?.enrollDate ?? '',
      allergy: student?.allergy ?? '',
      healthNote: student?.healthNote ?? '',
      emergencyContact: student?.emergencyContact ?? '',
      emergencyPhone: student?.emergencyPhone ?? '',
      mentorTeacherId: student?.mentorTeacherId ? String(student.mentorTeacherId) : '',
      currentStageId: student?.currentStageId ? String(student.currentStageId) : '',
      skipClass: true,
    });
  }, [open, student, branches, form]);

  const save = useMutation({
    mutationFn: async (values: StudentFormValues) => {
      const payload = {
        branchId: values.branchId,
        enrollNo: values.enrollNo,
        name: values.name,
        gender: values.gender,
        birthday: values.birthday || null,
        enrollDate: values.enrollDate || null,
        allergy: values.allergy || undefined,
        healthNote: values.healthNote || undefined,
        emergencyContact: values.emergencyContact || undefined,
        emergencyPhone: values.emergencyPhone || null,
        mentorTeacherId: values.mentorTeacherId,
        currentStageId: values.currentStageId || null,
      };
      if (editing && student) {
        return studentApi.update(student.id, { id: student.id, ...payload });
      }
      const created = await studentApi.create(payload);
      const guardianPhone = values.guardianPhone?.trim();
      if (guardianPhone) {
        await studentApi.upsertGuardian(created.id, {
          name: values.guardianName?.trim() || '家长',
          phone: guardianPhone,
          relation: values.guardianRelation?.trim() || '家长',
          isMainContact: 1,
        });
      }
      return created;
    },
    onSuccess: (saved) => {
      qc.invalidateQueries({ queryKey: ['students'] });
      if (editing && student) {
        void qc.invalidateQueries({ queryKey: ['student', String(student.id)] });
      }
      onSaved?.();
      onOpenChange(false);
      if (!editing) {
        toast.undo({
          message: '学员已创建',
          onUndo: () => {
            void studentApi.remove(saved.id).then(() => {
              qc.invalidateQueries({ queryKey: ['students'] });
              toast.info('已撤销创建');
            });
          },
        });
      } else {
        toast.success('已保存');
      }
    },
    onError: (e: Error) =>
      toast.error(
        e instanceof ApiError ? formatApiErrorMessage(e.payload, e.message) : e.message,
      ),
  });

  const showValidationErrors = (errors: FieldErrors<StudentFormValues>) => {
    const first = Object.values(errors).find((e) => e?.message);
    toast.error(first?.message?.toString() ?? '请完善表单信息');
    if (errors.branchId || errors.enrollNo || errors.name || errors.mentorTeacherId) setStep(0);
    else if (errors.emergencyPhone) setStep(1);
    else if (errors.guardianPhone || errors.guardianName) setStep(2);
  };

  const onSubmit = form.handleSubmit(
    (values) => save.mutate(values),
    showValidationErrors,
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="text-2xl font-semibold tracking-tight">
            {editing ? '编辑学员' : '新建学员'}
          </DialogTitle>
        </DialogHeader>
        <ol className="mb-4 flex gap-1 rounded-lg bg-muted p-1" aria-label="表单步骤">
          {STEPS.map((label, i) => (
            <li
              key={label}
              className={cn(
                'flex-1 rounded-md px-2 py-1.5 text-center text-xs font-medium',
                i === step ? 'bg-white text-foreground shadow-sm' : 'text-muted-fg',
              )}
            >
              {label}
            </li>
          ))}
        </ol>
        <form className="space-y-3" onSubmit={onSubmit}>
          {step === 0 ? (
            <>
              <div>
                <Label>校区</Label>
                <SimpleSelect
                  aria-label="校区"
                  value={form.watch('branchId') || undefined}
                  onValueChange={(v) => form.setValue('branchId', v, { shouldValidate: true })}
                  options={branches.map((b) => ({ value: String(b.id), label: b.name }))}
                  placeholder={branches.length ? '请选择校区' : '暂无校区，请先在系统设置中创建'}
                />
              </div>
              <div>
                <Label>入园编号</Label>
                <Input className="mt-1" {...form.register('enrollNo')} />
                {form.formState.errors.enrollNo ? (
                  <p className="mt-1 text-xs text-danger">{form.formState.errors.enrollNo.message}</p>
                ) : null}
              </div>
              <div>
                <Label>姓名</Label>
                <Input className="mt-1" {...form.register('name')} />
                {form.formState.errors.name ? (
                  <p className="mt-1 text-xs text-danger">{form.formState.errors.name.message}</p>
                ) : null}
              </div>
              <div>
                <Label>主带老师 *</Label>
                <SimpleSelect
                  aria-label="主带老师"
                  value={form.watch('mentorTeacherId') || undefined}
                  onValueChange={(v) =>
                    form.setValue('mentorTeacherId', v, { shouldValidate: true })
                  }
                  options={(teachersQuery.data ?? []).map((t) => ({
                    value: String(t.id),
                    label: t.name || t.username,
                  }))}
                  placeholder={teachersQuery.data?.length ? '请选择主带老师' : '该校区暂无老师，请先创建'}
                />
                {form.formState.errors.mentorTeacherId ? (
                  <p className="mt-1 text-xs text-danger">
                    {form.formState.errors.mentorTeacherId.message}
                  </p>
                ) : null}
              </div>
            </>
          ) : null}
          {step === 1 ? (
            <>
              <div>
                <Label>过敏史</Label>
                <Input className="mt-1" {...form.register('allergy')} />
              </div>
              <div>
                <Label>健康备注</Label>
                <Input className="mt-1" {...form.register('healthNote')} />
              </div>
              <div>
                <Label>紧急电话</Label>
                <Input className="mt-1" {...form.register('emergencyPhone')} />
              </div>
            </>
          ) : null}
          {step === 2 ? <GuardianSubForm register={form.register} /> : null}
          {step === 3 ? (
            <p className="text-sm text-muted-fg">暂不分班（阶段 G 启用班级分配）</p>
          ) : null}
          <DialogFooter className="gap-2 sm:justify-between">
            <Button
              type="button"
              variant="ghost"
              disabled={step === 0}
              onClick={() => setStep((s) => Math.max(0, s - 1))}
            >
              上一步
            </Button>
            {step < STEPS.length - 1 ? (
              <Button type="button" onClick={() => setStep((s) => s + 1)}>
                下一步
              </Button>
            ) : (
              <Button type="submit" disabled={save.isPending || branches.length === 0}>
                提交
              </Button>
            )}
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
