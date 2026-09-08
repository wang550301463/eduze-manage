import type { NavigateFunction } from 'react-router-dom';
import type { SearchResult } from '@/lib/searchClient';

/** 根据全局搜索结果跳转并携带 open* 查询参数，供列表页打开 Sheet */
export function searchHitUrl(hit: SearchResult): string | null {
  const id = encodeURIComponent(String(hit.entityId));
  switch (hit.group) {
    case 'student':
      return `/students?openId=${id}`;
    case 'class':
      return `/courses/class-groups?openId=${id}`;
    case 'lesson':
      return `/schedule?openId=${id}`;
    case 'guardian': {
      if (!hit.url.startsWith('/students?')) return null;
      const params = new URLSearchParams(hit.url.slice('/students?'.length));
      const studentId = params.get('openId');
      return studentId
        ? `/students?openId=${encodeURIComponent(studentId)}&openGuardianId=${id}`
        : null;
    }
  }
}

export function openSearchHit(hit: SearchResult, navigate: NavigateFunction): void {
  const url = searchHitUrl(hit);
  if (url) navigate(url);
}
