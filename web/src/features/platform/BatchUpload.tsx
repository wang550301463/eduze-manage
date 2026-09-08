import { useEffect, useState } from 'react';
import { processAttributedUploads, type AttributedUpload } from './batch-upload';
import { command, platformRequest, uploadMedia } from './client';
import type { Theme, RosterEntry, PortfolioRecord, DraftInput } from './types';
import { useAction } from './hooks';
import { Panel, Field, fieldClass, buttonClass, secondaryClass, Feedback } from './ui';
function Photo({ file }: { file: File }) {
  const [url, setUrl] = useState('');
  useEffect(() => {
    const objectUrl = URL.createObjectURL(file);
    setUrl(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [file]);
  return <img src={url} alt={file.name} className="h-28 w-full rounded-xl object-cover" />;
}
export function BatchUpload({ theme, roster }: { theme: Theme; roster: RosterEntry[] }) {
  const [queue, setQueue] = useState<AttributedUpload[]>([]);
  const action = useAction();
  const save = async (studentId: string, mediaId: string, fileName: string) => {
    const records = await platformRequest<PortfolioRecord[]>(
      `/portfolio/records?themeId=${theme.id}&studentId=${studentId}`,
    );
    const old = records[0];
    const content: DraftInput = old?.content ?? {
      themeId: theme.id,
      studentId,
      progress: 'IN_PROGRESS',
      classroomNote: '',
      comment: '',
      artworks: [],
      audioMediaIds: [],
    };
    if (content.artworks.some((a) => a.mediaIds.includes(mediaId))) return;
    await command(
      `/portfolio/records${old ? `/${old.id}` : ''}`,
      {
        ...content,
        version: old?.version,
        artworks: [
          ...content.artworks,
          {
            id: crypto.randomUUID(),
            title: fileName,
            kind: 'PROCESS',
            mediaIds: [mediaId],
            participantIds: [studentId],
            story: '',
          },
        ],
      },
      old ? 'PUT' : 'POST',
    );
  };
  return (
    <Panel>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="font-semibold">整班照片归属</h2>
          <p className="mt-1 text-xs text-muted-fg">
            先选择每张照片对应的学生，再批量存入云端草稿。
          </p>
        </div>
        <Field label="批量选择课堂照片">
          <input
            type="file"
            accept="image/*"
            multiple
            disabled={action.busy}
            onChange={(e) => {
              setQueue((old) => [
                ...old,
                ...Array.from(e.target.files ?? []).map((file) => ({
                  id: crypto.randomUUID(),
                  file,
                  studentId: '',
                  status: 'PENDING' as const,
                })),
              ]);
              e.target.value = '';
            }}
          />
        </Field>
      </div>
      <Feedback action={action} />
      <div className="mt-4 grid grid-cols-2 gap-3 md:grid-cols-4">
        {queue.map((q) => (
          <div key={q.id} className="grid gap-2 rounded-xl bg-white/70 p-2">
            <Photo file={q.file} />
            <p className="truncate text-xs">{q.file.name}</p>
            <select
              aria-label={`${q.file.name} 所属学生`}
              disabled={q.status === 'DONE' || action.busy}
              className={fieldClass}
              value={q.studentId}
              onChange={(e) =>
                setQueue((old) =>
                  old.map((i) => (i.id === q.id ? { ...i, studentId: e.target.value } : i)),
                )
              }
            >
              <option value="">请选择学生</option>
              {roster.map((r) => (
                <option key={r.studentId} value={r.studentId}>
                  {r.name}
                </option>
              ))}
            </select>
            <p
              className={q.status === 'FAILED' ? 'text-xs text-rose-700' : 'text-xs text-muted-fg'}
            >
              {q.status === 'DONE' ? '已存入学生草稿' : (q.error ?? '等待归属和上传')}
            </p>
          </div>
        ))}
      </div>
      {queue.length > 0 && (
        <div className="mt-4 flex gap-2">
          <button
            disabled={action.busy || queue.every((q) => q.status === 'DONE')}
            className={buttonClass}
            onClick={() =>
              void action.run(async () => {
                await processAttributedUploads(
                  queue,
                  (file) => uploadMedia(file, theme.content.branchId, 'ARTWORK'),
                  save,
                  setQueue,
                );
              }, '本批处理结束，请检查每张照片的状态')
            }
          >
            {action.busy ? '上传并保存中…' : '保存 / 重试未完成照片'}
          </button>
          <button
            disabled={action.busy}
            className={secondaryClass}
            onClick={() => setQueue((q) => q.filter((i) => i.status !== 'DONE'))}
          >
            移除已完成项
          </button>
        </div>
      )}
    </Panel>
  );
}
