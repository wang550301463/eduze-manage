import { useState } from 'react';
import { usePlatformList, useAction } from './hooks';
import { command } from './client';
import { Panel, Field, fieldClass, buttonClass, secondaryClass, Feedback, Notice } from './ui';
import { MediaPreview } from './MediaPreview';
import type { Publication, MediaLink } from './types';
import { assertMediaLimit, publicationMediaIds } from '../../../../packages/contracts/media';
export interface GrowthReport {
  id: string;
  title: string;
  summary: string;
  publications: Publication[];
  createdAt: string;
}
export function SnapshotViewer({ reports }: { reports: GrowthReport[] }) {
  const [opened, setOpened] = useState<{ publication: Publication; links: MediaLink[] } | null>(
    null,
  );
  const action = useAction();
  return (
    <div className="space-y-3">
      <Feedback action={action} />
      {reports.map((r) => (
        <div key={r.id} className="rounded-xl border border-white/70 bg-white/60 p-4">
          <h3 className="font-semibold">{r.title}</h3>
          <p className="my-2 whitespace-pre-wrap text-sm">{r.summary}</p>
          <div className="flex flex-wrap gap-2">
            {r.publications.map((p) => (
              <button
                key={p.id}
                className={secondaryClass}
                disabled={action.busy}
                onClick={() =>
                  void action.run(async () => {
                    const access = await command<{ items: MediaLink[] }>(
                      `/portfolio/reports/${r.id}/publications/${p.id}/media-access`,
                      { mediaIds: publicationMediaIds([p.content]) },
                    );
                    setOpened({ publication: p, links: access.items });
                  }, '已加载报告中的固定作品版本')
                }
              >
                {p.content.classroomNote ||
                  `学习回顾 ${new Date(p.createdAt).toLocaleDateString()}`}
              </button>
            ))}
          </div>
        </div>
      ))}
      {opened && (
        <Panel>
          <h3 className="mb-3 font-semibold">
            历史版本 · {new Date(opened.publication.createdAt).toLocaleDateString()}
          </h3>
          <p className="mb-3 text-sm">{opened.publication.content.classroomNote}</p>
          <div className="grid gap-3 md:grid-cols-2">
            {opened.links.map((link) => (
              <MediaPreview key={link.id} link={link} title="历史作品" />
            ))}
          </div>
          <p className="my-3 text-sm">{opened.publication.content.comment}</p>
          <button className={secondaryClass} onClick={() => setOpened(null)}>
            关闭历史预览
          </button>
        </Panel>
      )}
    </div>
  );
}
export function GrowthCollections({
  studentId,
  publications,
}: {
  studentId: string;
  publications: Publication[];
}) {
  const collections = usePlatformList<GrowthReport[]>(
    `/portfolio/students/${studentId}/collections`,
  );
  const [title, setTitle] = useState('');
  const [summary, setSummary] = useState('');
  const [selected, setSelected] = useState<string[]>([]);
  const [preview, setPreview] = useState<MediaLink[]>([]);
  const action = useAction();
  return (
    <Panel>
      <h2 className="mb-3 font-semibold">私人精选作品集</h2>
      <Notice error={collections.error} />
      <Feedback action={action} />
      <div className="grid gap-3">
        <Field label="精选集名称">
          <input className={fieldClass} value={title} onChange={(e) => setTitle(e.target.value)} />
        </Field>
        <Field label="精选集说明">
          <textarea
            className={fieldClass}
            value={summary}
            onChange={(e) => setSummary(e.target.value)}
          />
        </Field>
        <div className="flex flex-wrap gap-3">
          {publications.map((p) => (
            <label className="text-sm" key={p.id}>
              <input
                type="checkbox"
                checked={selected.includes(p.id)}
                onChange={(e) =>
                  setSelected(
                    e.target.checked ? [...selected, p.id] : selected.filter((id) => id !== p.id),
                  )
                }
              />{' '}
              {p.content.classroomNote || new Date(p.createdAt).toLocaleDateString()}
            </label>
          ))}
        </div>
        <button
          className={buttonClass}
          disabled={action.busy || !title.trim() || !selected.length}
          onClick={() =>
            void action.run(async () => {
              assertMediaLimit(
                publicationMediaIds(
                  publications.filter((p) => selected.includes(p.id)).map((p) => p.content),
                ),
              );
              await command(`/portfolio/students/${studentId}/collections`, {
                title,
                summary,
                publicationIds: selected,
              });
              setSelected([]);
              setTitle('');
              setSummary('');
            }, '私人精选集已保存')
          }
        >
          保存私人精选集
        </button>
      </div>
      {collections.data?.map((c) => (
        <div key={c.id} className="mt-4 rounded-xl bg-white/60 p-3">
          <h3 className="font-medium">{c.title}</h3>
          <p className="my-2 text-sm">{c.summary}</p>
          <div className="flex flex-wrap gap-2">
            {c.publications.map((p) => (
              <button
                key={p.id}
                className={secondaryClass}
                disabled={action.busy}
                onClick={() =>
                  void action.run(async () => {
                    const access = await command<{ items: MediaLink[] }>(
                      `/portfolio/publications/${p.id}/media-access`,
                      { mediaIds: publicationMediaIds([p.content]) },
                    );
                    setPreview(access.items);
                  }, '已加载精选作品')
                }
              >
                {p.content.classroomNote || '查看作品'}
              </button>
            ))}
          </div>
        </div>
      ))}
      {preview.length > 0 && (
        <div className="mt-4 grid gap-3 md:grid-cols-2">
          {preview.map((link) => (
            <MediaPreview key={link.id} link={link} title="精选作品" />
          ))}
        </div>
      )}
    </Panel>
  );
}
