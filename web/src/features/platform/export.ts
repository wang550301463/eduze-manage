import { platformRequest } from './client';
export async function downloadExhibitionExport(path: string): Promise<void> {
  const result = await platformRequest<{ svg: string; filename: string }>(path);
  const url = URL.createObjectURL(new Blob([result.svg], { type: 'image/svg+xml' }));
  try {
    const link = document.createElement('a');
    link.href = url;
    link.download = result.filename;
    link.click();
  } finally {
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
}
