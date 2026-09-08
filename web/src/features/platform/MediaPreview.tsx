import type { MediaLink } from './types';
export function MediaPreview({ link, title }: { link: MediaLink; title: string }) {
  return (
    <div>
      {link.contentType?.startsWith('video/') ? (
        <video src={link.url} controls className="max-h-72 w-full rounded-xl" />
      ) : link.contentType?.startsWith('audio/') ? (
        <audio src={link.url} controls />
      ) : (
        <img
          src={link.thumbnailUrl || link.url}
          alt={title}
          className="max-h-64 rounded-xl object-contain"
        />
      )}
      <a
        href={link.url}
        target="_blank"
        rel="noreferrer"
        className="text-xs text-primary underline"
      >
        打开原文件
      </a>
    </div>
  );
}
