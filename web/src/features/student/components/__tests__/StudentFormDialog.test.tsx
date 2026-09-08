import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, expect, it, vi } from 'vitest';
import { studentApi, teacherApi } from '../../api';
import type { Student } from '../../types';
import { StudentFormDialog } from '../StudentFormDialog';

afterEach(() => { cleanup(); vi.restoreAllMocks(); });

it('requires an explicit submit after navigating to the final step', async () => {
  const student: Student = {
    id: '11', branchId: '1', enrollNo: 'YS001', name: '验收学员', gender: 0,
    status: 1, mentorTeacherId: '2', classGroups: [], totalRemaining: 0, alertLow: false,
  };
  vi.spyOn(teacherApi, 'list').mockResolvedValue([{ id: '2', name: '老师', username: 'teacher' }]);
  const update = vi.spyOn(studentApi, 'update').mockResolvedValue(student);
  const onSaved = vi.fn();
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(<QueryClientProvider client={client}>
    <StudentFormDialog open onOpenChange={vi.fn()} onSaved={onSaved}
      student={student} branches={[{ id: '1', name: '总校区', code: 'HQ' }]} />
  </QueryClientProvider>);
  for (let step = 0; step < 3; step += 1) {
    await userEvent.click(screen.getByRole('button', { name: '下一步' }));
  }
  expect(screen.getByRole('button', { name: '提交' })).toBeInTheDocument();
  expect(update).not.toHaveBeenCalled();
  expect(onSaved).not.toHaveBeenCalled();
  await userEvent.click(screen.getByRole('button', { name: '提交' }));
  await waitFor(() => expect(update).toHaveBeenCalledTimes(1));
  expect(onSaved).toHaveBeenCalledTimes(1);
  client.clear();
});
