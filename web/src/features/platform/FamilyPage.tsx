import { usePlatformList, useAction } from './hooks';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { studentApi } from '@/features/student/api';
import { useAuthStore } from '@/features/auth/store';
import { command } from './client';
import type { FamilyBinding } from './types';
import { Panel, Field, fieldClass, buttonClass, secondaryClass, Feedback, Notice } from './ui';
export function FamilyPage() {
  const [keyword, setKeyword] = useState('');
  const [student, setStudent] = useState('');
  const [invite, setInvite] = useState<{ code: string; expiresAt: string } | null>(null);
  const user = useAuthStore((s) => s.user);
  const action = useAction();
  const students = useQuery({
    queryKey: ['platform', user?.id, 'student-search', keyword],
    queryFn: () => studentApi.list({ keyword, size: '50' }),
  });
  const bindings = usePlatformList<FamilyBinding[]>(
    `/academic/family/bindings?studentId=${student}`,
    Boolean(student),
  );
  return (
    <div className="space-y-5">
      <h1 className="text-2xl font-semibold">家庭绑定与授权</h1>
      <p className="text-sm text-muted-fg">
        前台发出邀请，家长在小程序申请，确认身份后开通孩子的内容访问。
      </p>
      <Feedback action={action} />
      <Notice error={students.error || bindings.error} />
      <Panel>
        <div className="grid gap-3 md:grid-cols-2">
          <Field label="搜索学员">
            <input
              className={fieldClass}
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </Field>
          <Field label="选择学员">
            <select
              className={fieldClass}
              value={student}
              onChange={(e) => {
                setStudent(e.target.value);
                setInvite(null);
              }}
            >
              <option value="">请选择</option>
              {students.data?.records.map((s) => (
                <option key={s.id} value={String(s.id)}>
                  {s.name}
                </option>
              ))}
            </select>
          </Field>
        </div>
        <button
          className={`${buttonClass} mt-4`}
          disabled={!student || action.busy}
          onClick={() =>
            void action.run(
              async () =>
                setInvite(await command('/academic/family/invites', { studentId: student })),
              '邀请已生成',
            )
          }
        >
          生成有时效的家庭邀请
        </button>
        {invite && (
          <div className="mt-4 rounded-xl bg-white p-4">
            <p className="text-sm">请将邀请码交给已核验的监护人</p>
            <p className="my-2 select-all font-mono text-2xl">{invite.code}</p>
            <p className="text-xs">有效期至 {new Date(invite.expiresAt).toLocaleString()}</p>
          </div>
        )}
      </Panel>
      {student && (
        <Panel>
          <h2 className="mb-4 font-semibold">绑定申请</h2>
          {bindings.data?.map((b) => (
            <div
              key={b.id}
              className="flex flex-wrap items-center justify-between gap-2 border-b py-3"
            >
              <div>
                <p className="text-sm">家庭账号 {b.userId}</p>
                <p className="text-xs text-muted-fg">
                  {b.status === 'ACTIVE'
                    ? '已授权'
                    : b.status === 'PENDING'
                      ? '等待前台核验'
                      : '已解除'}
                </p>
              </div>
              <div className="flex gap-2">
                {b.status === 'PENDING' && (
                  <button
                    disabled={action.busy}
                    className={buttonClass}
                    onClick={() =>
                      void action.run(
                        () => command(`/academic/family/bindings/${b.id}/approve`, {}),
                        '家庭访问已开通',
                      )
                    }
                  >
                    身份已核验，批准
                  </button>
                )}
                {['PENDING', 'ACTIVE'].includes(b.status) && (
                  <button
                    disabled={action.busy}
                    className={secondaryClass}
                    onClick={() =>
                      void action.run(
                        () => command(`/academic/family/bindings/${b.id}`, {}, 'DELETE'),
                        '授权已解除',
                      )
                    }
                  >
                    解除授权
                  </button>
                )}
              </div>
            </div>
          ))}
          {bindings.data?.length === 0 && <p className="text-sm">还没有家庭绑定申请。</p>}
        </Panel>
      )}
    </div>
  );
}
