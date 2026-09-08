export interface AttributedUpload {
  id: string;
  studentId: string;
  file: File;
  status: 'PENDING' | 'DONE' | 'FAILED';
  mediaId?: string;
  error?: string;
}
export async function processAttributedUploads(
  queue: AttributedUpload[],
  upload: (file: File) => Promise<string>,
  save: (studentId: string, mediaId: string, fileName: string) => Promise<void>,
  updated?: (queue: AttributedUpload[]) => void,
): Promise<AttributedUpload[]> {
  const result = queue.map((item) => ({ ...item }));
  for (const item of result) {
    if (item.status === 'DONE') continue;
    try {
      if (!item.studentId) throw new Error('请选择作品所属学生');
      item.mediaId ??= await upload(item.file);
      await save(item.studentId, item.mediaId, item.file.name);
      item.status = 'DONE';
      item.error = undefined;
    } catch (e) {
      item.status = 'FAILED';
      item.error = e instanceof Error ? e.message : '保存失败，请重试';
    }
    updated?.(result.map((item) => ({ ...item })));
  }
  return result;
}
