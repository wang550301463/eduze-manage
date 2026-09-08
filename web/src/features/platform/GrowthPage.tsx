import { GrowthCollections, SnapshotViewer, type GrowthReport } from './GrowthCollections';
import { downloadExhibitionExport } from './export';
import { assertMediaLimit, publicationMediaIds } from '../../../../packages/contracts/media';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { studentApi } from '@/features/student/api';
import { useAuthStore } from '@/features/auth/store';
import type { Publication, Exhibition } from './types';
import { command } from './client';
import { usePlatformList, useAction } from './hooks';
import { Panel, Field, fieldClass, buttonClass, secondaryClass, Feedback, Notice } from './ui';
interface Growth {
  studentId: string;
  publications: Publication[];
  reports: GrowthReport[];
}
export function GrowthPage() {
  const [keyword, setKeyword] = useState('');
  const [student, setStudent] = useState('');
  const [selected, setSelected] = useState<Publication[]>([]);
  const [title, setTitle] = useState('');
  const [summary, setSummary] = useState('');
  const [startsAt, setStartsAt] = useState('');
  const [endsAt, setEndsAt] = useState('');
  const user = useAuthStore((s) => s.user);
  const action = useAction();
  const students = useQuery({
    queryKey: ['platform', user?.id, 'growth-students', keyword],
    queryFn: () => studentApi.list({ keyword, size: '50' }),
  });
  const growth = usePlatformList<Growth>(`/portfolio/students/${student}/growth`, Boolean(student));
  const exhibitions = usePlatformList<Exhibition[]>('/portfolio/exhibitions');
  return (
    <div className="space-y-5">
      <h1 className="text-2xl font-semibold">成长报告与作品展</h1>
      <p className="text-sm text-muted-fg">
        从已发布的主题回顾选取证据；公开展览另行取得家庭授权并审核。
      </p>
      <Feedback action={action} />
      <Notice error={students.error || growth.error || exhibitions.error} />
      <Panel>
        <div className="grid gap-3 md:grid-cols-2">
          <Field label="搜索学员">
            <input
              className={fieldClass}
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </Field>
          <Field label="成长档案">
            <select
              className={fieldClass}
              value={student}
              onChange={(e) => setStudent(e.target.value)}
            >
              <option value="">选择学生</option>
              {students.data?.records.map((s) => (
                <option key={s.id} value={String(s.id)}>
                  {s.name}
                </option>
              ))}
            </select>
          </Field>
        </div>
        {growth.data?.publications.map((p) => (
          <label className="mt-3 flex gap-2 rounded-xl bg-white/60 p-3 text-sm" key={p.id}>
            <input
              type="checkbox"
              checked={selected.some((s) => s.id === p.id)}
              onChange={(e) =>
                setSelected(
                  e.target.checked ? [...selected, p] : selected.filter((s) => s.id !== p.id),
                )
              }
            />
            <span>
              {p.content.classroomNote || '主题课效'}
              <span className="block text-xs text-muted-fg">
                {p.content.comment} · {new Date(p.createdAt).toLocaleDateString()}
              </span>
            </span>
          </label>
        ))}
        {student && <SnapshotViewer key={student} reports={growth.data?.reports ?? []} />}
      </Panel>
      {student && (
        <GrowthCollections
          key={student}
          studentId={student}
          publications={growth.data?.publications ?? []}
        />
      )}
      <Panel>
        <h2 className="mb-3 font-semibold">已选择 {selected.length} 份主题学习回顾</h2>
        <div className="grid gap-3">
          <Field label="报告或展览标题">
            <input
              className={fieldClass}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </Field>
          <Field label="阶段回顾或展览前言">
            <textarea
              className={fieldClass}
              value={summary}
              onChange={(e) => setSummary(e.target.value)}
            />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="展览开始时间（可选）">
              <input
                type="datetime-local"
                className={fieldClass}
                value={startsAt}
                onChange={(e) => setStartsAt(e.target.value)}
              />
            </Field>
            <Field label="展览结束时间（可选）">
              <input
                type="datetime-local"
                className={fieldClass}
                value={endsAt}
                min={startsAt}
                onChange={(e) => setEndsAt(e.target.value)}
              />
            </Field>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              className={buttonClass}
              disabled={
                action.busy ||
                !student ||
                !title ||
                !selected.length ||
                selected.some((p) => p.content.studentId !== student)
              }
              onClick={() =>
                void action.run(() => {
                  assertMediaLimit(publicationMediaIds(selected.map((p) => p.content)));
                  return command(`/portfolio/students/${student}/reports`, {
                    title,
                    summary,
                    publicationIds: selected.map((p) => p.id),
                  });
                }, '成长报告已保存')
              }
            >
              生成该学生成长报告
            </button>
            <button
              className={secondaryClass}
              disabled={action.busy || !title || !selected.length}
              onClick={() =>
                void action.run(
                  () =>
                    command('/portfolio/exhibitions', {
                      title,
                      introduction: summary,
                      startsAt: startsAt ? new Date(startsAt).toISOString() : null,
                      endsAt: endsAt ? new Date(endsAt).toISOString() : null,
                      publicationIds: selected.map((p) => p.id),
                    }),
                  '已创建待审核展览；公开前须完成监护人授权',
                )
              }
            >
              创建作品展草稿
            </button>
            <button className={secondaryClass} onClick={() => setSelected([])}>
              清空选择
            </button>
          </div>
        </div>
      </Panel>
      <div className="grid gap-4 lg:grid-cols-2">
        {exhibitions.data?.map((e) => (
          <Panel key={e.id}>
            <p className="text-xs text-primary">
              {e.status === 'PUBLISHED'
                ? '公开展览'
                : e.status === 'DRAFT'
                  ? '待审核'
                  : e.status === 'ARCHIVED'
                    ? '已归档'
                    : '已下架'}
            </p>
            <h2 className="mt-2 text-lg font-semibold">{e.title}</h2>
            <p className="my-3 text-sm">{e.introduction}</p>
            <div className="flex gap-2">
              {e.status === 'DRAFT' && (
                <button
                  className={buttonClass}
                  disabled={action.busy}
                  onClick={() =>
                    void action.run(
                      () => command(`/portfolio/exhibitions/${e.id}/approve`, {}),
                      '展览已通过审核',
                    )
                  }
                >
                  审核并公开
                </button>
              )}
              {(e.status === 'PUBLISHED' || e.status === 'ARCHIVED') && (
                <button
                  className={secondaryClass}
                  disabled={action.busy}
                  onClick={() =>
                    void action.run(
                      () =>
                        downloadExhibitionExport(
                          `/portfolio/public/exhibitions/${e.id}/share-card`,
                        ),
                      '分享卡已导出',
                    )
                  }
                >
                  导出分享卡
                </button>
              )}
              {e.status === 'PUBLISHED' && (
                <button
                  className={secondaryClass}
                  disabled={action.busy}
                  onClick={() =>
                    void action.run(
                      () => command(`/portfolio/exhibitions/${e.id}/archive`, {}),
                      '展览已归档',
                    )
                  }
                >
                  归档
                </button>
              )}
              {e.status === 'PUBLISHED' && (
                <button
                  className={secondaryClass}
                  disabled={action.busy}
                  onClick={() =>
                    void action.run(
                      () => command(`/portfolio/exhibitions/${e.id}/withdraw`, {}),
                      '展览已下架',
                    )
                  }
                >
                  下架展览
                </button>
              )}
            </div>
          </Panel>
        ))}
      </div>
    </div>
  );
}
