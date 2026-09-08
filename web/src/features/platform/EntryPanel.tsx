import { useState } from 'react';
import { command, uploadMedia } from './client';
import type { ClassroomEntry, Theme } from './types';
import { useAction, usePlatformList } from './hooks';
import { Field, fieldClass, buttonClass, secondaryClass, Feedback, Notice } from './ui';
export function EntryPanel({
  recordId,
  themeId,
  branchId,
}: {
  recordId: string;
  themeId: string;
  branchId: string;
}) {
  const [lessonId, setLessonId] = useState('');
  const [notes, setNotes] = useState('');
  const [mediaIds, setMediaIds] = useState<string[]>([]);
  const [key, setKey] = useState(() => crypto.randomUUID());
  const [reason, setReason] = useState('');
  const action = useAction();
  const entries = usePlatformList<ClassroomEntry[]>(`/portfolio/records/${recordId}/entries`);
  const theme = usePlatformList<Theme>(`/teaching/themes/${themeId}`);
  return (
    <section className="space-y-3 border-t pt-4">
      <h3 className="font-semibold">每次课堂动态</h3>
      <p className="text-xs text-muted-fg">
        课堂动态单独保存和发布。完整主题课效可以在几次课结束后再发布。
      </p>
      <Notice error={entries.error || theme.error} />
      <Feedback action={action} />
      <Field label="对应课堂">
        <select
          className={fieldClass}
          value={lessonId}
          onChange={(e) => setLessonId(e.target.value)}
        >
          <option value="">选择本次课堂</option>
          {theme.data?.content.lessonIds.map((id, i) => (
            <option key={id} value={id}>
              第 {i + 1} 次关联课堂
            </option>
          ))}
        </select>
      </Field>
      <Field label="本次课堂观察">
        <textarea className={fieldClass} value={notes} onChange={(e) => setNotes(e.target.value)} />
      </Field>
      <Field label="本次过程照片">
        <input
          type="file"
          multiple
          accept="image/*"
          disabled={action.busy}
          onChange={(e) => {
            const files = Array.from(e.target.files ?? []);
            void action.run(async () => {
              for (const file of files) {
                const id = await uploadMedia(file, branchId, 'PORTFOLIO');
                setMediaIds((old) => [...old, id]);
              }
            }, '课堂照片已上传');
          }}
        />
      </Field>
      <p className="text-xs text-muted-fg">本次已上传 {mediaIds.length} 张过程照片</p>
      <button
        className={secondaryClass}
        disabled={action.busy || !lessonId || !notes.trim()}
        onClick={() =>
          void action.run(async () => {
            await command(`/portfolio/records/${recordId}/entries`, {
              lessonId,
              occurredAt: new Date().toISOString(),
              notes,
              mediaIds,
              idempotencyKey: key,
            });
            setNotes('');
            setMediaIds([]);
            setKey(crypto.randomUUID());
          }, '课堂动态草稿已保存')
        }
      >
        保存本次课堂草稿
      </button>
      {entries.data?.map((entry) => (
        <div key={entry.id} className="rounded-xl bg-white/70 p-3">
          <p className="text-xs text-muted-fg">
            {new Date(entry.occurredAt).toLocaleString()} ·{' '}
            {entry.status === 'PUBLISHED'
              ? '家庭可见'
              : entry.status === 'DRAFT'
                ? '草稿'
                : '已撤回'}
          </p>
          <p className="my-2 whitespace-pre-wrap text-sm">{entry.notes}</p>
          <p className="mb-2 text-xs text-muted-fg">{entry.mediaIds.length} 张过程照片</p>
          {entry.status === 'DRAFT' && (
            <button
              className={buttonClass}
              disabled={action.busy}
              onClick={() =>
                void action.run(
                  () =>
                    command(`/portfolio/records/${recordId}/entries/${entry.id}/publish`, {
                      idempotencyKey: `entry-publish-${entry.id}`,
                    }),
                  '本次课堂动态已发布给家庭',
                )
              }
            >
              发布这次课堂动态
            </button>
          )}
          {entry.status === 'PUBLISHED' && (
            <div className="flex gap-2">
              <input
                className={fieldClass}
                aria-label="课堂动态撤回原因"
                placeholder="撤回原因"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
              <button
                className={secondaryClass}
                disabled={action.busy || !reason.trim()}
                onClick={() =>
                  void action.run(
                    () =>
                      command(`/portfolio/records/${recordId}/entries/${entry.id}/withdraw`, {
                        reason,
                      }),
                    '课堂动态已撤回',
                  )
                }
              >
                撤回
              </button>
            </div>
          )}
        </div>
      ))}
    </section>
  );
}
