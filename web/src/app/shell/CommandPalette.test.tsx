import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CommandPalette } from './CommandPalette';
import { ShellProvider, useShell } from './shell-context';
import { search, type SearchResponse } from '@/lib/searchClient';
import { useAuthStore } from '@/features/auth/store';

vi.mock('@/lib/searchClient', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/lib/searchClient')>()),
  search: vi.fn(),
}));

function Launcher() {
  const { setPaletteOpen } = useShell();
  return (
    <>
      <button onClick={() => setPaletteOpen(true)}>打开搜索</button>
      <CommandPalette />
    </>
  );
}
function setup() {
  render(
    <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <ShellProvider>
        <Launcher />
      </ShellProvider>
    </MemoryRouter>,
  );
  fireEvent.click(screen.getByRole('button', { name: '打开搜索' }));
}
async function type(query: string) {
  fireEvent.change(screen.getByTestId('command-palette-input'), { target: { value: query } });
  await act(async () => {
    await vi.advanceTimersByTimeAsync(300);
  });
}
const response = (title: string): SearchResponse => ({
  results: [
    {
      id: `student-${title}`,
      entityId: '9007199254740993123',
      group: 'student',
      title,
      url: '/students/9007199254740993123',
    },
  ],
});

describe('global search interactions', () => {
  beforeEach(() => {
    useAuthStore.setState({
      permissions: ['student:read', 'guardian:read', 'classgroup:read', 'lesson:read'],
    });
    vi.useFakeTimers();
    vi.mocked(search).mockReset();
    vi.stubGlobal(
      'ResizeObserver',
      class {
        observe() {}
        unobserve() {}
        disconnect() {}
      },
    );
  });
  afterEach(() => {
    cleanup();
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('names the search dialog for assistive technology', () => {
    setup();
    expect(screen.getByRole('dialog', { name: '全局搜索' })).toBeInTheDocument();
  });

  it('ignores stale responses after a newer keyword is entered', async () => {
    let resolveFirst!: (value: SearchResponse) => void;
    vi.mocked(search)
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveFirst = resolve;
          }),
      )
      .mockResolvedValueOnce(response('新结果'));
    setup();
    await type('旧');
    await type('新');
    expect(screen.getByText('新结果')).toBeInTheDocument();
    await act(async () => {
      resolveFirst(response('旧结果'));
    });
    expect(screen.queryByText('旧结果')).not.toBeInTheDocument();
    expect(screen.getByText('新结果')).toBeInTheDocument();
  });

  it('clears pending results when the keyword is cleared', async () => {
    let resolveSearch!: (value: SearchResponse) => void;
    vi.mocked(search).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveSearch = resolve;
        }),
    );
    setup();
    await type('关键词');
    await type('');
    await act(async () => {
      resolveSearch(response('过期结果'));
    });
    expect(screen.queryByText('过期结果')).not.toBeInTheDocument();
    expect(screen.queryByTestId('command-palette-loading')).not.toBeInTheDocument();
  });

  it('distinguishes errors from no matches and retries the same query', async () => {
    vi.mocked(search)
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce(response('重试结果'));
    setup();
    await type('查询');
    expect(screen.getByRole('alert')).toHaveTextContent('搜索暂时不可用');
    expect(screen.queryByText('未找到相关结果')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '重试搜索' }));
    await act(async () => {
      await vi.advanceTimersByTimeAsync(300);
    });
    expect(screen.getByText('重试结果')).toBeInTheDocument();
  });

  it('shows the contact details of a guardian without a linked student', async () => {
    vi.mocked(search).mockResolvedValue({
      results: [
        {
          id: 'guardian-8',
          entityId: '8',
          group: 'guardian',
          title: '林家长',
          subtitle: '13800000000',
          url: '/guardians/8',
        },
      ],
    });
    setup();
    await type('林');
    fireEvent.click(screen.getByTestId('search-hit-guardian-8'));
    expect(screen.getByRole('region', { name: '家长联系信息' })).toHaveTextContent('13800000000');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it.each([
    {
      role: 'teacher',
      permissions: ['student:read', 'lesson:read'],
      visible: ['student', 'lesson'],
      hidden: ['guardian', 'class'],
    },
    {
      role: 'frontdesk',
      permissions: ['student:read', 'guardian:read', 'classgroup:read'],
      visible: ['student', 'guardian', 'class'],
      hidden: ['lesson'],
    },
  ])('shows only destinations the $role can read', async ({ permissions, visible, hidden }) => {
    useAuthStore.setState({ permissions });
    vi.mocked(search).mockResolvedValue({
      results: [
        { id: 'student-1', entityId: '1', group: 'student', title: '学员结果', url: '/students/1' },
        {
          id: 'guardian-2',
          entityId: '2',
          group: 'guardian',
          title: '家长结果',
          url: '/students?openId=1&openGuardianId=2',
        },
        { id: 'class-3', entityId: '3', group: 'class', title: '班级结果', url: '/class-groups/3' },
        { id: 'lesson-4', entityId: '4', group: 'lesson', title: '课次结果', url: '/lessons/4' },
      ],
    });
    setup();
    await type('结果');
    for (const group of visible)
      expect(screen.getByTestId(new RegExp(`search-hit-${group}-`))).toBeInTheDocument();
    for (const group of hidden)
      expect(screen.queryByTestId(new RegExp(`search-hit-${group}-`))).not.toBeInTheDocument();
  });

  it('allows guardian-only readers to view unlinked contacts while hiding linked student destinations', async () => {
    useAuthStore.setState({ permissions: ['guardian:read'] });
    vi.mocked(search).mockResolvedValue({
      results: [
        {
          id: 'guardian-1',
          entityId: '1',
          group: 'guardian',
          title: '独立家长',
          subtitle: '13800000000',
          url: '/guardians/1',
        },
        {
          id: 'guardian-2',
          entityId: '2',
          group: 'guardian',
          title: '关联家长',
          url: '/students?openId=3&openGuardianId=2',
        },
      ],
    });
    setup();
    await type('家长');
    expect(screen.queryByText('关联家长')).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId('search-hit-guardian-1'));
    expect(screen.getByRole('region', { name: '家长联系信息' })).toHaveTextContent('13800000000');
  });
});
