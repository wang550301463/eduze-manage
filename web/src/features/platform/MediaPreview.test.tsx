import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { MediaPreview } from './MediaPreview';
import { viewLinks } from '../../../../miniapp/core/media';
describe('thumbnail versus original media', () => {
  const link = {
    id: 'm',
    url: 'https://media.example/original',
    thumbnailUrl: 'https://media.example/thumb',
    expiresAt: '2026-09-08T09:00:00Z',
    contentType: 'image/jpeg',
  };
  it('renders the small image but opens the original file', () => {
    render(<MediaPreview link={link} title="作品" />);
    expect(screen.getByRole('img')).toHaveAttribute('src', link.thumbnailUrl);
    expect(screen.getByRole('link', { name: '打开原文件' })).toHaveAttribute('href', link.url);
  });
  it('keeps original url for native preview and uses thumbnail only as image display source', () => {
    const [image, video, fallback] = viewLinks([
      link,
      { ...link, id: 'v', contentType: 'video/mp4' },
      { id: 'f', url: link.url, contentType: 'image/png' },
    ]);
    expect(image).toMatchObject({ url: link.url, displayUrl: link.thumbnailUrl });
    expect(video).toMatchObject({ url: link.url, displayUrl: link.url, kind: 'video' });
    expect(fallback.displayUrl).toBe(link.url);
  });
});
