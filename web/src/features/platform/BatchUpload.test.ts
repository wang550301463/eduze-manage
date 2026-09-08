import { describe, it, expect, vi } from 'vitest';
import { processAttributedUploads } from './batch-upload';
describe('whole class photo attribution', () => {
  it('keeps each photo with its selected student and retries only unfinished files', async () => {
    const save = vi.fn().mockResolvedValue(undefined);
    const upload = vi
      .fn()
      .mockResolvedValueOnce('media-a')
      .mockRejectedValueOnce(new Error('offline'));
    const queue = [
      {
        id: 'a',
        studentId: 'student-a',
        file: new File(['a'], 'a.jpg'),
        status: 'PENDING' as const,
      },
      {
        id: 'b',
        studentId: 'student-b',
        file: new File(['b'], 'b.jpg'),
        status: 'PENDING' as const,
      },
    ];
    const first = await processAttributedUploads(queue, upload, save);
    expect(save).toHaveBeenCalledWith('student-a', 'media-a', 'a.jpg');
    expect(first.map((q) => q.status)).toEqual(['DONE', 'FAILED']);
    upload.mockResolvedValue('media-b');
    const second = await processAttributedUploads(first, upload, save);
    expect(upload).toHaveBeenCalledTimes(3);
    expect(save).toHaveBeenLastCalledWith('student-b', 'media-b', 'b.jpg');
    expect(second.every((q) => q.status === 'DONE')).toBe(true);
  });
  it('does not upload unassigned photos', async () => {
    const upload = vi.fn();
    const result = await processAttributedUploads(
      [{ id: 'a', studentId: '', file: new File(['a'], 'a.jpg'), status: 'PENDING' }],
      upload,
      vi.fn(),
    );
    expect(upload).not.toHaveBeenCalled();
    expect(result[0].status).toBe('FAILED');
  });
});
