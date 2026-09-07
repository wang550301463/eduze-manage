import { describe, expect, it } from 'vitest';
import { navItemEnd } from '@/app/shell/nav-config';

describe('navItemEnd', () => {
  it('marks root as end', () => {
    expect(navItemEnd('/')).toBe(true);
  });

  it('marks parent paths that have sibling nested nav items as end', () => {
    expect(navItemEnd('/attendance')).toBe(true);
    expect(navItemEnd('/courses')).toBe(true);
  });

  it('does not mark leaf paths as end', () => {
    expect(navItemEnd('/attendance/leaves')).toBe(false);
    expect(navItemEnd('/courses/class-groups')).toBe(false);
    expect(navItemEnd('/students')).toBe(false);
    expect(navItemEnd('/lesson-history')).toBe(false);
  });
});
