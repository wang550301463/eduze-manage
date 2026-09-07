import type { NavigateFunction } from 'react-router-dom';
import type { SearchResult } from '@/lib/searchClient';

/** 根据全局搜索结果跳转并携带 open* 查询参数，供列表页打开 Sheet */
export function openSearchHit(hit: SearchResult, navigate: NavigateFunction): void {
  navigate(hit.url);
}
