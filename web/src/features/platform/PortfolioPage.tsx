import { assertMediaLimit, publicationMediaIds } from '../../../../packages/contracts/media';
import { usePlatformList, useAction } from './hooks';
import { MediaPreview } from './MediaPreview';
import type { MediaLink } from './types';
import { EntryPanel } from './EntryPanel';
import { BatchUpload } from './BatchUpload';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuthStore } from '@/features/auth/store';
import type { PortfolioRecord, DraftInput, Theme, RosterEntry, Progress, Artwork } from './types';
import { command, uploadMedia, platformRequest } from './client';
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
const progressLabels: Record<Progress, string> = {
  NOT_STARTED: '未开始',
  IN_PROGRESS: '创作中',
  MAKEUP_PENDING: '待补课',
  COMPLETED: '已完成',
};
const emptyDraft = (themeId: string, studentId: string): DraftInput => ({
  themeId,
  studentId,
  progress: 'IN_PROGRESS',
  classroomNote: '',
  comment: '',
  artworks: [],
  audioMediaIds: [],
});
export function PortfolioPage() {
  const [params] = useSearchParams();
  const [branch, setBranch] = useState('');
  const [theme, setTheme] = useState(params.get('themeId') ?? '');
  const [record, setRecord] = useState<PortfolioRecord | null>(null);
  const [common, setCommon] = useState('');
  const [selected, setSelected] = useState<string[]>([]);
  const user = useAuthStore((s) => s.user);
  const action = useAction();
  const themes = usePlatformList<Theme[]>(`/teaching/themes?branchId=${branch}`);
  const roster = usePlatformList<RosterEntry[]>(
    `/portfolio/themes/${theme}/roster`,
    Boolean(theme),
  );
  const open = (row: RosterEntry) =>
    action.run(
      async () =>
        setRecord(
          row.recordId
            ? await platformRequest<PortfolioRecord>(`/portfolio/records/${row.recordId}`)
            : await command<PortfolioRecord>(
                '/portfolio/records',
                emptyDraft(theme, row.studentId),
              ),
        ),
      '已打开云端草稿',
    );
  return (
    <div key={user?.id} className="space-y-5">
      <div>
        <p className="text-xs uppercase tracking-[.2em] text-primary">Learning stories</p>
        <h1 className="mt-2 text-2xl font-semibold">作品与课效</h1>
        <p className="mt-2 text-sm text-muted-fg">
          过程、成品与观察汇成一个主题。个别学生可以在补课后继续完成。
        </p>
      </div>
      <Panel>
        <div className="grid gap-3 md:grid-cols-2">
          <BranchPicker
            value={branch}
            onChange={(v) => {
              setBranch(v);
              setTheme('');
              setRecord(null);
              setSelected([]);
            }}
          />
          <Field label="班级主题">
            <select
              className={fieldClass}
              value={theme}
              onChange={(e) => {
                setTheme(e.target.value);
                setRecord(null);
                setSelected([]);
              }}
            >
              <option value="">选择进行中的主题</option>
              {themes.data?.map((t) => (
                <option value={t.id} key={t.id}>
                  {t.content.title} ·{' '}
                  {t.status === 'FINISHED'
                    ? '已结束'
                    : t.status === 'IN_PROGRESS'
                      ? '进行中'
                      : '计划中'}
                </option>
              ))}
            </select>
          </Field>
        </div>
      </Panel>
      <Feedback action={action} />
      <Notice error={themes.error || roster.error} />
      {theme && themes.data?.find((t) => t.id === theme) && (
        <BatchUpload
          key={theme}
          theme={themes.data.find((t) => t.id === theme)!}
          roster={roster.data ?? []}
        />
      )}
      {theme && (
        <div className="grid gap-5 xl:grid-cols-[320px_1fr]">
          <Panel>
            <h2 className="mb-3 font-semibold">学生课效清单</h2>
            <div className="space-y-2">
              {roster.data?.map((row) => (
                <div
                  key={row.studentId}
                  className="flex items-center gap-2 rounded-xl bg-white/60 p-3"
                >
                  <input
                    type="checkbox"
                    aria-label={`选择${row.name}`}
                    checked={selected.includes(row.studentId)}
                    onChange={(e) =>
                      setSelected(
                        e.target.checked
                          ? [...selected, row.studentId]
                          : selected.filter((s) => s !== row.studentId),
                      )
                    }
                  />
                  <button className="flex-1 text-left" onClick={() => void open(row)}>
                    <span className="block font-medium">{row.name}</span>
                    <span className="text-xs text-muted-fg">
                      {progressLabels[row.progress] ?? '未记录'} ·{' '}
                      {row.needsPublishing
                        ? '有更新待发布'
                        : row.status === 'PUBLISHED'
                          ? '已发布'
                          : row.status === 'WITHDRAWN'
                            ? '已撤回'
                            : '待整理'}
                    </span>
                  </button>
                </div>
              ))}
            </div>
            {roster.data?.length === 0 && (
              <p className="text-sm">该班暂无学生，请先在教务中维护班级名单。</p>
            )}
            <div className="mt-5 grid gap-2">
              <Field label="批量课堂说明">
                <textarea
                  className={fieldClass}
                  value={common}
                  onChange={(e) => setCommon(e.target.value)}
                  placeholder="选中学生后添加本主题公共说明"
                />
              </Field>
              <button
                className={secondaryClass}
                disabled={action.busy || !selected.length}
                onClick={() =>
                  void action.run(async () => {
                    for (const studentId of selected) {
                      const row = roster.data?.find((r) => r.studentId === studentId);
                      const old = row?.recordId
                        ? await platformRequest<PortfolioRecord>(
                            `/portfolio/records/${row.recordId}`,
                          )
                        : null;
                      await command(
                        `/portfolio/records${old ? `/${old.id}` : ''}`,
                        {
                          ...(old ? old.content : emptyDraft(theme, studentId)),
                          version: old?.version,
                          classroomNote: common,
                        },
                        old ? 'PUT' : 'POST',
                      );
                    }
                    setRecord(null);
                  }, '已批量保存草稿；请逐位预览后发布')
                }
              >
                应用到 {selected.length} 位学生草稿
              </button>
            </div>
          </Panel>
          {record ? (
            <PortfolioEditor
              key={`${record.id}:${record.version}`}
              record={record}
              roster={roster.data ?? []}
              onSaved={setRecord}
            />
          ) : (
            <Panel>
              <h2 className="font-semibold">选择学生开始整理</h2>
              <p className="mt-3 text-sm text-muted-fg">
                老师手机上传的记录会出现在这里。每个学生拥有独立的创作进度和家庭课效。
              </p>
            </Panel>
          )}
        </div>
      )}
    </div>
  );
}
export function PortfolioEditor({
  record,
  roster,
  onSaved,
}: {
  record: PortfolioRecord;
  roster: RosterEntry[];
  onSaved: (record: PortfolioRecord) => void;
}) {
  const [draft, setDraft] = useState({ ...record.content, version: record.version });
  const [dirty, setDirty] = useState(false);
  const [preview, setPreview] = useState(false);
  const [reason, setReason] = useState('');
  const [urls, setUrls] = useState<Record<string, MediaLink>>({});
  const [failed, setFailed] = useState<File[]>([]);
  const action = useAction();
  const change = (update: Partial<DraftInput>) => {
    setDraft((old) => ({ ...old, ...update }));
    setDirty(true);
    setPreview(false);
  };
  const artwork = (index: number, update: Partial<Artwork>) =>
    change({ artworks: draft.artworks.map((a, i) => (i === index ? { ...a, ...update } : a)) });
  const upload = (files: File[]) =>
    action.run(async () => {
      assertMediaLimit(publicationMediaIds([draft]), files.length);
      setFailed([]);
      let failure: unknown;
      for (const file of files) {
        try {
          const id = await uploadMedia(file, record.branchId, 'PORTFOLIO');
          setDraft((old) => ({
            ...old,
            artworks: [
              ...old.artworks,
              {
                id: crypto.randomUUID(),
                title: file.name,
                kind: 'PROCESS',
                mediaIds: [id],
                participantIds: [record.content.studentId],
                story: '',
              },
            ],
          }));
          setDirty(true);
          setPreview(false);
        } catch (e) {
          setFailed((old) => [...old, file]);
          failure = e;
        }
      }
      if (failure) throw failure;
    }, '上传完成，请确认每件作品归属并保存');
  const showPreview = () =>
    action.run(async () => {
      const ids = [...draft.artworks.flatMap((a) => a.mediaIds), ...draft.audioMediaIds];
      if (ids.length) {
        const access = await command<{ items: MediaLink[] }>(
          `/portfolio/records/${record.id}/media-access`,
          { mediaIds: ids },
        );
        setUrls(Object.fromEntries(access.items.map((i) => [i.id, i])));
      }
      setPreview(true);
    }, '已加载家庭预览');
  return (
    <Panel>
      <div className="space-y-4">
        <div className="flex justify-between">
          <h2 className="font-semibold">
            {roster.find((r) => r.studentId === record.content.studentId)?.name ?? '学生'}的主题记录
          </h2>
          <span className="text-xs text-muted-fg">
            云端版本 {record.version}
            {dirty ? ' · 有未保存修改' : ''}
          </span>
        </div>
        <Feedback action={action} />
        <Field label="创作进度">
          <select
            className={fieldClass}
            value={draft.progress}
            onChange={(e) => change({ progress: e.target.value as Progress })}
          >
            {Object.entries(progressLabels).map(([v, l]) => (
              <option key={v} value={v}>
                {l}
              </option>
            ))}
          </select>
        </Field>
        <Field label="课堂说明">
          <textarea
            className={fieldClass}
            value={draft.classroomNote}
            onChange={(e) => change({ classroomNote: e.target.value })}
          />
        </Field>
        <Field label="个性点评">
          <textarea
            className={fieldClass}
            value={draft.comment}
            onChange={(e) => change({ comment: e.target.value })}
          />
        </Field>
        <Field label="上传过程或成品（可多选）">
          <input
            type="file"
            multiple
            accept="image/*,video/*"
            disabled={action.busy}
            onChange={(e) => void upload(Array.from(e.target.files ?? []))}
          />
        </Field>
        {failed.length > 0 && (
          <button className={secondaryClass} onClick={() => void upload(failed)}>
            重试失败文件（{failed.length}）
          </button>
        )}
        <Field label="上传原始音频观察">
          <input
            type="file"
            accept="audio/*"
            disabled={action.busy}
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file)
                void action.run(async () => {
                  const id = await uploadMedia(file, record.branchId, 'PORTFOLIO');
                  change({ audioMediaIds: [...draft.audioMediaIds, id] });
                }, '音频已上传，请保存草稿');
            }}
          />
        </Field>
        <p className="text-xs text-muted-fg">已附加 {draft.audioMediaIds.length} 段原始音频</p>
        {draft.artworks.map((a, i) => (
          <div key={a.id} className="grid gap-2 rounded-2xl bg-white/70 p-3">
            <div className="grid grid-cols-2 gap-2">
              <Field label={`作品 ${i + 1} 名称`}>
                <input
                  className={fieldClass}
                  value={a.title}
                  onChange={(e) => artwork(i, { title: e.target.value })}
                />
              </Field>
              <Field label={`作品 ${i + 1} 阶段`}>
                <select
                  className={fieldClass}
                  value={a.kind}
                  onChange={(e) => artwork(i, { kind: e.target.value as Artwork['kind'] })}
                >
                  <option value="PROCESS">过程</option>
                  <option value="FINAL">成品</option>
                </select>
              </Field>
            </div>
            <Field label="作品故事">
              <textarea
                className={fieldClass}
                value={a.story}
                onChange={(e) => artwork(i, { story: e.target.value })}
              />
            </Field>
            <fieldset>
              <legend className="text-sm">合作学生（本人始终保留）</legend>
              <div className="mt-2 flex flex-wrap gap-3">
                {roster.map((r) => (
                  <label className="text-sm" key={r.studentId}>
                    <input
                      type="checkbox"
                      disabled={r.studentId === draft.studentId}
                      checked={a.participantIds.includes(r.studentId)}
                      onChange={(e) =>
                        artwork(i, {
                          participantIds: e.target.checked
                            ? [...a.participantIds, r.studentId]
                            : a.participantIds.filter((id) => id !== r.studentId),
                        })
                      }
                    />{' '}
                    {r.name}
                  </label>
                ))}
              </div>
            </fieldset>
            <button
              className="text-left text-sm text-rose-700"
              onClick={() => change({ artworks: draft.artworks.filter((_, j) => i !== j) })}
            >
              从本记录移除
            </button>
          </div>
        ))}
        <div className="flex flex-wrap gap-2">
          <button
            disabled={action.busy || !dirty}
            className={buttonClass}
            onClick={() =>
              void action.run(
                async () =>
                  onSaved(
                    await command<PortfolioRecord>(`/portfolio/records/${record.id}`, draft, 'PUT'),
                  ),
                '草稿已保存',
              )
            }
          >
            保存草稿
          </button>
          <button
            disabled={action.busy || dirty}
            className={secondaryClass}
            onClick={() => void showPreview()}
          >
            预览课效
          </button>
          <button
            className={secondaryClass}
            disabled={action.busy}
            onClick={() =>
              void action.run(
                async () =>
                  onSaved(
                    await platformRequest<PortfolioRecord>(`/portfolio/records/${record.id}`),
                  ),
                '已重新载入最新草稿',
              )
            }
          >
            重新打开云端版本
          </button>
        </div>
        {dirty && (
          <p className="text-xs text-muted-fg">
            先保存草稿，再预览和发布；重新打开云端版本会放弃当前未保存修改。
          </p>
        )}
        {preview && (
          <section className="space-y-3 rounded-2xl border border-primary/20 bg-white p-4">
            <h3 className="font-semibold">家庭课效预览</h3>
            <p className="whitespace-pre-wrap text-sm">{draft.classroomNote}</p>
            {draft.artworks.map((a) => (
              <div key={a.id}>
                {a.mediaIds.map(
                  (id) => urls[id] && <MediaPreview key={id} link={urls[id]} title={a.title} />,
                )}
                <p>
                  {a.title} · {a.kind === 'FINAL' ? '成品' : '过程'}
                </p>
                <p className="text-sm">{a.story}</p>
              </div>
            ))}
            {draft.audioMediaIds.map(
              (id) => urls[id] && <MediaPreview key={id} link={urls[id]} title="课堂音频观察" />,
            )}
            <p className="whitespace-pre-wrap text-sm">{draft.comment}</p>
            <button
              className={buttonClass}
              disabled={action.busy}
              onClick={() =>
                void action.run(async () => {
                  await command(`/portfolio/records/${record.id}/publish`, {
                    version: record.version,
                    idempotencyKey: `publish-${record.id}-${record.version}`,
                  });
                  setPreview(false);
                  onSaved(
                    await platformRequest<PortfolioRecord>(`/portfolio/records/${record.id}`),
                  );
                }, '课效已发布到授权家庭')
              }
            >
              确认发布给家庭
            </button>
          </section>
        )}
        <EntryPanel
          recordId={record.id}
          themeId={record.content.themeId}
          branchId={record.branchId}
        />
        {record.status === 'PUBLISHED' && (
          <div className="grid gap-2 border-t pt-4">
            <Field label="撤回原因">
              <input
                className={fieldClass}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
            </Field>
            <button
              disabled={action.busy || !reason.trim()}
              className={secondaryClass}
              onClick={() =>
                void action.run(
                  async () =>
                    onSaved(
                      await command<PortfolioRecord>(`/portfolio/records/${record.id}/withdraw`, {
                        version: record.version,
                        reason,
                      }),
                    ),
                  '课效已撤回',
                )
              }
            >
              撤回家庭课效
            </button>
          </div>
        )}
      </div>
    </Panel>
  );
}
