import { apiClient } from '@/lib/axios';
import type { ApiResponse } from '@/lib/api-types';

export type SearchResultGroup = 'student' | 'guardian' | 'class' | 'lesson';

export type SearchResult = {
  id: string;
  entityId: number;
  group: SearchResultGroup;
  title: string;
  subtitle?: string;
  url: string;
};

export type SearchResponse = {
  results: SearchResult[];
};

type BackendHit = {
  type: 'student' | 'guardian' | 'class_group' | 'lesson';
  id: number;
  title: string;
  subtitle?: string;
  url: string;
  branch?: string | null;
  branchId?: number | null;
};

const GROUP_MAP: Record<BackendHit['type'], SearchResultGroup> = {
  student: 'student',
  guardian: 'guardian',
  class_group: 'class',
  lesson: 'lesson',
};

export const searchGroupLabels: Record<SearchResultGroup, string> = {
  student: '学员',
  guardian: '家长',
  class: '班级',
  lesson: '课次',
};

function mapHit(hit: BackendHit): SearchResult {
  const group = GROUP_MAP[hit.type];
  return {
    id: `${group}-${hit.id}`,
    entityId: hit.id,
    group,
    title: hit.title,
    subtitle: hit.subtitle,
    url: hit.url,
  };
}

export async function search(query: string, limit = 20): Promise<SearchResponse> {
  const { data: body } = await apiClient.get<ApiResponse<BackendHit[]>>('/search', {
    params: { q: query.trim(), limit },
  });
  const hits = body.data ?? [];
  return { results: hits.map(mapHit) };
}
