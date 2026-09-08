import { usePlatformList, useAction } from './hooks';
import { useState } from 'react';
import type {
  Template,
  TemplateInput,
  Resource,
  ResourceInput,
  TemplateVersion,
  MediaLink,
} from './types';
import { command, uploadMedia } from './client';
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

const emptyTemplate = (): TemplateInput => ({
  title: '',
  ageMin: 4,
  ageMax: 8,
  goals: '',
  materials: '',
  expectedLessons: 3,
  steps: [
    { title: '观察与构图', content: '' },
    { title: '色彩与塑造', content: '' },
    { title: '表达与分享', content: '' },
  ],
  tags: [],
  mediaIds: [],
});
const emptyResource = (): ResourceInput => ({
  title: '',
  kind: 'PDF',
  description: '',
  mediaIds: [],
  tags: [],
});
export function TeachingPage() {
  const [tab, setTab] = useState<'templates' | 'resources'>('templates');
  const [search, setSearch] = useState('');
  const [editing, setEditing] = useState<string | null>(null);
  const [draft, setDraft] = useState(emptyTemplate);
  const [resource, setResource] = useState(emptyResource);
  const [branch, setBranch] = useState('');
  const [preview, setPreview] = useState<{
    kind: string;
    items: MediaLink[];
  } | null>(null);
  const templates = usePlatformList<Template[]>(
    `/teaching/templates?q=${encodeURIComponent(search)}`,
  );
  const resources = usePlatformList<Resource[]>(
    `/teaching/resources?q=${encodeURIComponent(search)}`,
  );
  const versions = usePlatformList<TemplateVersion[]>(
    `/teaching/template-versions?q=${encodeURIComponent(search)}`,
  );
  const action = useAction();
  const save = () =>
    action.run(async () => {
      await command(
        `/teaching/${tab}${editing && editing !== 'new' ? `/${editing}` : ''}`,
        tab === 'templates' ? draft : resource,
        editing === 'new' ? 'POST' : 'PUT',
      );
      setEditing(null);
    });
  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-[.2em] text-primary">Teaching studio</p>
          <h1 className="mt-2 text-2xl font-semibold">教案与课件</h1>
          <p className="mt-2 text-sm text-muted-fg">把一个创作主题，组织成连续几次有目标的课堂。</p>
        </div>
        <button
          className={buttonClass}
          onClick={() => {
            setEditing('new');
            setDraft(emptyTemplate());
            setResource(emptyResource());
          }}
        >
          新建{tab === 'templates' ? '主题' : '课件'}
        </button>
      </div>
      <div className="flex gap-2">
        <button
          className={tab === 'templates' ? buttonClass : secondaryClass}
          onClick={() => {
            setTab('templates');
            setEditing(null);
          }}
        >
          教学主题
        </button>
        <button
          className={tab === 'resources' ? buttonClass : secondaryClass}
          onClick={() => {
            setTab('resources');
            setEditing(null);
          }}
        >
          课件资源
        </button>
        <input
          aria-label="搜索主题或课件"
          placeholder="搜索名称或标签"
          className={`${fieldClass} max-w-xs`}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>
      <Feedback action={action} />
      {preview && (
        <Panel>
          <div className="flex justify-between">
            <h2 className="font-semibold">课件预览</h2>
            <button className={secondaryClass} onClick={() => setPreview(null)}>
              关闭预览
            </button>
          </div>
          {preview.items.map((item) => (
            <div key={item.id} className="mt-3">
              {preview.kind === 'IMAGE' ? (
                <img
                  src={item.thumbnailUrl || item.url}
                  alt="课件"
                  className="max-h-96 rounded-xl object-contain"
                />
              ) : preview.kind === 'VIDEO' ? (
                <video src={item.url} controls className="max-h-96 w-full" />
              ) : preview.kind === 'AUDIO' ? (
                <audio src={item.url} controls />
              ) : preview.kind === 'PDF' ? (
                <iframe title="PDF 课件预览" src={item.url} className="h-96 w-full" />
              ) : null}
              <a
                href={item.url}
                target="_blank"
                rel="noreferrer"
                className="text-sm text-primary underline"
              >
                打开附件
              </a>
            </div>
          ))}
        </Panel>
      )}
      <Notice error={templates.error || resources.error} />
      {editing && (
        <Panel>
          <form
            className="grid gap-4"
            onSubmit={(e) => {
              e.preventDefault();
              void save();
            }}
          >
            {tab === 'templates' ? (
              <>
                <Field label="主题名称">
                  <input
                    required
                    className={fieldClass}
                    value={draft.title}
                    onChange={(e) => setDraft({ ...draft, title: e.target.value })}
                  />
                </Field>
                <div className="grid grid-cols-3 gap-3">
                  <Field label="最小年龄">
                    <input
                      type="number"
                      min="1"
                      className={fieldClass}
                      value={draft.ageMin}
                      onChange={(e) => setDraft({ ...draft, ageMin: Number(e.target.value) })}
                    />
                  </Field>
                  <Field label="最大年龄">
                    <input
                      type="number"
                      min={draft.ageMin}
                      className={fieldClass}
                      value={draft.ageMax}
                      onChange={(e) => setDraft({ ...draft, ageMax: Number(e.target.value) })}
                    />
                  </Field>
                  <Field label="预计课次">
                    <input
                      type="number"
                      min="1"
                      max="30"
                      className={fieldClass}
                      value={draft.expectedLessons}
                      onChange={(e) =>
                        setDraft({ ...draft, expectedLessons: Number(e.target.value) })
                      }
                    />
                  </Field>
                </div>
                <Field label="教学目标">
                  <textarea
                    required
                    className={fieldClass}
                    value={draft.goals}
                    onChange={(e) => setDraft({ ...draft, goals: e.target.value })}
                  />
                </Field>
                <Field label="材料清单">
                  <textarea
                    className={fieldClass}
                    value={draft.materials}
                    onChange={(e) => setDraft({ ...draft, materials: e.target.value })}
                  />
                </Field>
                {draft.steps.map((step, i) => (
                  <div key={i} className="grid gap-2 rounded-xl bg-white/60 p-3">
                    <Field label={`第 ${i + 1} 课次标题`}>
                      <input
                        className={fieldClass}
                        value={step.title}
                        onChange={(e) =>
                          setDraft({
                            ...draft,
                            steps: draft.steps.map((s, j) =>
                              j === i ? { ...s, title: e.target.value } : s,
                            ),
                          })
                        }
                      />
                    </Field>
                    <Field label={`第 ${i + 1} 课次内容`}>
                      <textarea
                        className={fieldClass}
                        value={step.content}
                        onChange={(e) =>
                          setDraft({
                            ...draft,
                            steps: draft.steps.map((s, j) =>
                              j === i ? { ...s, content: e.target.value } : s,
                            ),
                          })
                        }
                      />
                    </Field>
                    <button
                      type="button"
                      className="text-left text-sm text-rose-700"
                      onClick={() =>
                        setDraft({ ...draft, steps: draft.steps.filter((_, j) => j !== i) })
                      }
                    >
                      移除此步骤
                    </button>
                  </div>
                ))}
                <button
                  type="button"
                  className={secondaryClass}
                  onClick={() =>
                    setDraft({ ...draft, steps: [...draft.steps, { title: '', content: '' }] })
                  }
                >
                  增加课次步骤
                </button>
                <Field label="标签（逗号分隔）">
                  <input
                    className={fieldClass}
                    value={draft.tags.join(',')}
                    onChange={(e) => setDraft({ ...draft, tags: e.target.value.split(',') })}
                  />
                </Field>
              </>
            ) : (
              <>
                <Field label="课件名称">
                  <input
                    required
                    className={fieldClass}
                    value={resource.title}
                    onChange={(e) => setResource({ ...resource, title: e.target.value })}
                  />
                </Field>
                <Field label="类型">
                  <select
                    className={fieldClass}
                    value={resource.kind}
                    onChange={(e) =>
                      setResource({ ...resource, kind: e.target.value as ResourceInput['kind'] })
                    }
                  >
                    {Object.entries({
                      IMAGE: '图片',
                      PDF: 'PDF',
                      VIDEO: '视频',
                      PRESENTATION: '演示文件',
                      AUDIO: '音频',
                    }).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label="使用说明">
                  <textarea
                    className={fieldClass}
                    value={resource.description}
                    onChange={(e) => setResource({ ...resource, description: e.target.value })}
                  />
                </Field>
                <BranchPicker value={branch} onChange={setBranch} />
                <Field label="上传课件">
                  <input
                    type="file"
                    multiple
                    disabled={!branch || action.busy}
                    onChange={(e) => {
                      const files = Array.from(e.target.files ?? []);
                      void action.run(async () => {
                        for (const file of files) {
                          const id = await uploadMedia(file, branch, 'TEACHING');
                          setResource((old) => ({ ...old, mediaIds: [...old.mediaIds, id] }));
                        }
                      }, '文件已上传，可保存草稿');
                    }}
                  />
                </Field>
                <p className="text-sm">
                  已完成上传 {resource.mediaIds.length} 个文件；失败文件可重新选择上传。
                </p>
              </>
            )}
            <div className="flex gap-2">
              <button disabled={action.busy} className={buttonClass}>
                保存{tab === 'templates' ? '主题' : '课件'}草稿
              </button>
              <button type="button" className={secondaryClass} onClick={() => setEditing(null)}>
                关闭
              </button>
            </div>
          </form>
        </Panel>
      )}
      {tab === 'templates' && (
        <Panel>
          <h2 className="font-semibold">机构公共主题版本</h2>
          <p className="my-2 text-sm text-muted-fg">
            选择已发布版本复制为自己的草稿。后续修改不会覆盖原教案。
          </p>
          {versions.error && <p role="alert">{versions.error.message}</p>}
          <div className="grid gap-3 lg:grid-cols-2">
            {versions.data?.map((v) => (
              <div key={v.id} className="rounded-xl bg-white/70 p-3">
                <h3 className="font-medium">
                  {v.content.title} · v{v.version}
                </h3>
                <p className="my-2 text-sm">{v.content.goals}</p>
                <button
                  disabled={action.busy}
                  className={secondaryClass}
                  onClick={() =>
                    void action.run(async () => {
                      const copied = await command<Template>(
                        `/teaching/template-versions/${v.id}/copy`,
                        {},
                      );
                      setEditing(copied.id);
                      setDraft({ ...copied.content, version: copied.version });
                    }, '已创建具有媒体授权的私人主题草稿')
                  }
                >
                  复制此发布版本
                </button>
              </div>
            ))}
          </div>
        </Panel>
      )}
      <div className="grid gap-4 lg:grid-cols-2">
        {tab === 'templates'
          ? (templates.data ?? []).map((t) => (
              <Panel key={t.id}>
                <p className="text-xs text-primary">
                  {t.content.ageMin}–{t.content.ageMax} 岁 · 预计 {t.content.expectedLessons} 课次 ·
                  草稿 v{t.version}
                </p>
                <h2 className="mt-2 text-lg font-semibold">{t.content.title}</h2>
                <p className="my-3 whitespace-pre-wrap text-sm text-muted-fg">{t.content.goals}</p>
                <div className="flex flex-wrap gap-2">
                  <button
                    className={secondaryClass}
                    onClick={() => {
                      setEditing(t.id);
                      setDraft({ ...t.content, version: t.version });
                    }}
                  >
                    编辑
                  </button>

                  <button
                    disabled={action.busy}
                    className={buttonClass}
                    onClick={() =>
                      void action.run(
                        () =>
                          command(`/teaching/templates/${t.id}/publish`, { version: t.version }),
                        '已发布固定版本，可以安排到班级',
                      )
                    }
                  >
                    发布版本
                  </button>
                </div>
              </Panel>
            ))
          : (resources.data ?? []).map((r) => (
              <Panel key={r.id}>
                <p className="text-xs text-primary">
                  {r.content.kind} · {r.published ? '已发布' : '草稿'} · v{r.version}
                </p>
                <h2 className="my-2 text-lg font-semibold">{r.content.title}</h2>
                <p className="mb-3 text-sm">
                  {r.content.description} · {r.content.mediaIds.length} 个附件
                </p>
                <div className="flex gap-2">
                  <button
                    disabled={action.busy}
                    className={secondaryClass}
                    onClick={() =>
                      void action.run(async () => {
                        const access = await command<{ items: MediaLink[] }>(
                          `/teaching/resources/${r.id}/media-access`,
                          { mediaIds: r.content.mediaIds },
                        );
                        setPreview({ kind: r.content.kind, items: access.items });
                      }, '已加载课件预览')
                    }
                  >
                    预览附件
                  </button>
                  <button
                    className={secondaryClass}
                    disabled={action.busy}
                    onClick={() => {
                      if (!r.published) {
                        setEditing(r.id);
                        setResource({ ...r.content, version: r.version });
                        return;
                      }
                      void action.run(async () => {
                        const copied = await command<Resource>(
                          `/teaching/resources/${r.id}/copy`,
                          {},
                        );
                        setEditing(copied.id);
                        setResource({ ...copied.content, version: copied.version });
                      }, '已创建具有媒体授权的私人课件草稿');
                    }}
                  >
                    {r.published ? '复制新版' : '编辑'}
                  </button>
                  {!r.published && (
                    <button
                      className={buttonClass}
                      disabled={action.busy}
                      onClick={() =>
                        void action.run(
                          () =>
                            command(`/teaching/resources/${r.id}/publish`, { version: r.version }),
                          '课件版本已发布',
                        )
                      }
                    >
                      发布
                    </button>
                  )}
                </div>
              </Panel>
            ))}
      </div>
      {(tab === 'templates' ? templates.isLoading : resources.isLoading) && (
        <p role="status">正在加载…</p>
      )}
      {!(tab === 'templates' ? templates.isLoading : resources.isLoading) &&
        (tab === 'templates' ? templates.data : resources.data)?.length === 0 && (
          <Panel>
            <p>还没有{tab === 'templates' ? '教学主题' : '课件'}，从第一份教学资料开始。</p>
          </Panel>
        )}
    </div>
  );
}
