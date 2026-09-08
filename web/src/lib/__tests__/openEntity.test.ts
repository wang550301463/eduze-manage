import { describe, expect, it, vi } from 'vitest';
import { openSearchHit } from '@/lib/openEntity';
import type { SearchResult } from '@/lib/searchClient';

describe('search result navigation', () => {
  it.each([
    ['student', '/students/9007199254740993123', '/students?openId=9007199254740993123'],
    [
      'class',
      '/class-groups/9007199254740993123',
      '/courses/class-groups?openId=9007199254740993123',
    ],
    ['lesson', '/lessons/9007199254740993123', '/schedule?openId=9007199254740993123'],
    [
      'guardian',
      '/students?openId=81&openGuardianId=9007199254740993123',
      '/students?openId=81&openGuardianId=9007199254740993123',
    ],
  ] as const)(
    'opens %s on a supported frontend route without rounding IDs',
    (group, url, expected) => {
      const navigate = vi.fn();
      const hit = {
        id: `${group}-9007199254740993123`,
        entityId: '9007199254740993123',
        group,
        title: '测试',
        url,
      };
      openSearchHit(hit as SearchResult, navigate);
      expect(navigate).toHaveBeenCalledWith(expected);
    },
  );

  it('does not navigate an unlinked guardian to a missing page', () => {
    const navigate = vi.fn();
    openSearchHit(
      {
        id: 'guardian-8',
        entityId: '8',
        group: 'guardian',
        title: '家长',
        url: '/guardians/8',
      } as SearchResult,
      navigate,
    );
    expect(navigate).not.toHaveBeenCalled();
  });
});
