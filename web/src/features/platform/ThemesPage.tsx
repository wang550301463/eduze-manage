import { usePlatformList, useAction } from './hooks';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { listClassGroups } from '@/features/course/api';
import { fetchWeekSchedule } from '@/features/lesson/api';
import { useAuthStore } from '@/features/auth/store';
import type { Theme, ThemeInput, TemplateVersion, Plan } from './types';
import { command } from './client';
import {
  Panel,
  Field,
  fieldClass,
  buttonClass,
  secondaryClass,
  Feedback,
  Notice,
  BranchPicker,
} from './ui';
const blankTheme = (branchId: string): ThemeInput => ({
  branchId,
  groupId: '',
  templateVersionId: '',
  title: '',
  lessonIds: [],
  handoffNote: '',
});
export function ThemesPage() {
  const [branch, setBranch] = useState('');
  const [editing, setEditing] = useState<string | null>(null);
  const [draft, setDraft] = useState(blankTheme(''));
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [plan, setPlan] = useState({
    name: '',
    startsOn: '',
    endsOn: '',
    themeIds: [] as string[],
  });
  const [reason, setReason] = useState('');
  const user = useAuthStore((s) => s.user);
  const action = useAction();
  const themes = usePlatformList<Theme[]>(`/teaching/themes?branchId=${branch}`);
  const versions = usePlatformList<TemplateVersion[]>('/teaching/template-versions');
  const plans = usePlatformList<Plan[]>(`/teaching/plans?branchId=${branch}`, Boolean(branch));
  const groups = useQuery({
    queryKey: ['platform', user?.id, 'groups', branch],
    queryFn: () => listClassGroups(1, 100, branch),
    enabled: Boolean(branch),
  });
  const lessons = useQuery({
    queryKey: ['platform', user?.id, 'lessons', branch, date],
    queryFn: () => fetchWeekSchedule(Number(branch), date),
    enabled: Boolean(branch),
  });
  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-xs tracking-[.2em] text-primary">CLASS JOURNEY</p>
          <h1 className="mt-2 text-2xl font-semibold">班级教学计划</h1>
          <p className="mt-2 text-sm text-muted-fg">
            主题按班独立推进，延长课次和代课交接都有记录。
          </p>
        </div>
        <button
          className={buttonClass}
          disabled={!branch}
          onClick={() => {
            setEditing('new');
            setDraft(blankTheme(branch));
          }}
        >
          安排班级主题
        </button>
      </div>
      <BranchPicker
        value={branch}
        onChange={(v) => {
          setBranch(v);
          setEditing(null);
          setDraft(blankTheme(v));
        }}
      />
      <Feedback action={action} />
      <Notice
        error={themes.error || versions.error || groups.error || lessons.error || plans.error}
      />
      {editing && (
        <Panel>
          <form
            className="grid gap-4"
            onSubmit={(e) => {
              e.preventDefault();
              void action.run(async () => {
                await command(
                  `/teaching/themes${editing === 'new' ? '' : `/${editing}`}`,
                  draft,
                  editing === 'new' ? 'POST' : 'PUT',
                );
                setEditing(null);
              });
            }}
          >
            <Field label="主题名称">
              <input
                className={fieldClass}
                required
                value={draft.title}
                onChange={(e) => setDraft({ ...draft, title: e.target.value })}
              />
            </Field>
            <div className="grid gap-3 md:grid-cols-2">
              <Field label="固定班">
                <select
                  required
                  className={fieldClass}
                  value={draft.groupId}
                  onChange={(e) => setDraft({ ...draft, groupId: e.target.value, lessonIds: [] })}
                >
                  <option value="">选择班级</option>
                  {groups.data?.items.map((g) => (
                    <option key={g.id} value={String(g.id)}>
                      {g.name}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="采用教学版本">
                <select
                  required
                  disabled={editing !== 'new'}
                  className={fieldClass}
                  value={draft.templateVersionId}
                  onChange={(e) => {
                    const v = versions.data?.find((v) => v.id === e.target.value);
                    setDraft({
                      ...draft,
                      templateVersionId: e.target.value,
                      title: v?.content.title ?? draft.title,
                    });
                  }}
                >
                  <option value="">选择已发布教案</option>
                  {versions.data?.map((v) => (
                    <option key={v.id} value={v.id}>
                      {v.content.title} · v{v.version}
                    </option>
                  ))}
                </select>
              </Field>
            </div>
            <Field label="查看课次日期">
              <input
                type="date"
                className={fieldClass}
                value={date}
                onChange={(e) => setDate(e.target.value)}
              />
            </Field>
            <fieldset className="rounded-xl bg-white/60 p-3">
              <legend>关联实际课堂（可跨周选择）</legend>
              <div className="grid gap-2">
                {lessons.data?.days
                  .flatMap((d) => d.lessons)
                  .filter((l) => String(l.classGroupId) === draft.groupId)
                  .map((l) => (
                    <label className="text-sm" key={l.id}>
                      <input
                        type="checkbox"
                        checked={draft.lessonIds.includes(String(l.id))}
                        onChange={(e) =>
                          setDraft({
                            ...draft,
                            lessonIds: e.target.checked
                              ? [...draft.lessonIds, String(l.id)]
                              : draft.lessonIds.filter((id) => id !== String(l.id)),
                          })
                        }
                      />{' '}
                      {l.startAt.replace('T', ' ').slice(0, 16)} · {l.teacherShortName}
                    </label>
                  ))}
              </div>
              <p className="mt-2 text-sm">已关联 {draft.lessonIds.length} 节课</p>
            </fieldset>
            <Field label="进度与代课交接">
              <textarea
                className={fieldClass}
                value={draft.handoffNote}
                onChange={(e) => setDraft({ ...draft, handoffNote: e.target.value })}
              />
            </Field>
            <div className="flex gap-2">
              <button className={buttonClass} disabled={action.busy}>
                保存教学安排
              </button>
              <button type="button" className={secondaryClass} onClick={() => setEditing(null)}>
                关闭
              </button>
            </div>
          </form>
        </Panel>
      )}
      <div className="grid gap-4 lg:grid-cols-2">
        {themes.data?.map((t) => (
          <Panel key={t.id}>
            <p className="text-xs text-primary">
              {t.status === 'PLANNED' ? '计划中' : t.status === 'IN_PROGRESS' ? '进行中' : '已结束'}{' '}
              · 已关联 {t.content.lessonIds.length} 课次
            </p>
            <h2 className="my-2 text-lg font-semibold">{t.content.title}</h2>
            <p className="mb-3 whitespace-pre-wrap text-sm text-muted-fg">
              {t.content.handoffNote || t.template.goals}
            </p>
            <div className="flex flex-wrap gap-2">
              <Link className={buttonClass} to={`/portfolio?themeId=${t.id}`}>
                整理学生课效
              </Link>
              <button
                className={secondaryClass}
                onClick={() => {
                  setEditing(t.id);
                  setDraft({ ...t.content, version: t.version });
                }}
              >
                课次与交接
              </button>
              {t.status !== 'FINISHED' ? (
                <button
                  disabled={action.busy}
                  className={secondaryClass}
                  onClick={() =>
                    void action.run(() =>
                      command(`/teaching/themes/${t.id}/transition`, {
                        version: t.version,
                        status: t.status === 'PLANNED' ? 'IN_PROGRESS' : 'FINISHED',
                        reason: '教师更新课堂进度',
                      }),
                    )
                  }
                >
                  {t.status === 'PLANNED' ? '开始教学' : '结束班级主题'}
                </button>
              ) : (
                <>
                  <input
                    aria-label="重开原因"
                    className={fieldClass}
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder="延长主题的原因"
                  />
                  <button
                    className={secondaryClass}
                    disabled={!reason.trim() || action.busy}
                    onClick={() =>
                      void action.run(() =>
                        command(`/teaching/themes/${t.id}/transition`, {
                          version: t.version,
                          status: 'IN_PROGRESS',
                          reason,
                        }),
                      )
                    }
                  >
                    重开并延长
                  </button>
                </>
              )}
            </div>
          </Panel>
        ))}
      </div>
      {branch && (
        <Panel>
          <h2 className="mb-3 font-semibold">学期计划</h2>
          {plans.data?.map((p) => (
            <p key={p.id} className="mb-2 text-sm">
              {p.name} · {p.startsOn} 至 {p.endsOn} · {p.themeIds.length} 个主题
            </p>
          ))}
          <form
            className="grid gap-3"
            onSubmit={(e) => {
              e.preventDefault();
              void action.run(async () => {
                await command('/teaching/plans', { ...plan, branchId: branch });
                setPlan({ name: '', startsOn: '', endsOn: '', themeIds: [] });
              });
            }}
          >
            <Field label="计划名称">
              <input
                required
                className={fieldClass}
                value={plan.name}
                onChange={(e) => setPlan({ ...plan, name: e.target.value })}
              />
            </Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="开始日期">
                <input
                  required
                  type="date"
                  className={fieldClass}
                  value={plan.startsOn}
                  onChange={(e) => setPlan({ ...plan, startsOn: e.target.value })}
                />
              </Field>
              <Field label="结束日期">
                <input
                  required
                  type="date"
                  min={plan.startsOn}
                  className={fieldClass}
                  value={plan.endsOn}
                  onChange={(e) => setPlan({ ...plan, endsOn: e.target.value })}
                />
              </Field>
            </div>
            <div className="flex flex-wrap gap-3">
              {themes.data?.map((t) => (
                <label className="text-sm" key={t.id}>
                  <input
                    type="checkbox"
                    checked={plan.themeIds.includes(t.id)}
                    onChange={(e) =>
                      setPlan({
                        ...plan,
                        themeIds: e.target.checked
                          ? [...plan.themeIds, t.id]
                          : plan.themeIds.filter((id) => id !== t.id),
                      })
                    }
                  />{' '}
                  {t.content.title}
                </label>
              ))}
            </div>
            <button disabled={action.busy} className={buttonClass}>
              保存学期计划
            </button>
          </form>
        </Panel>
      )}
    </div>
  );
}
