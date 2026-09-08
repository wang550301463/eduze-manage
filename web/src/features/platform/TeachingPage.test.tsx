import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, it, expect, vi } from 'vitest';
import { TeachingPage } from './TeachingPage';
import * as client from './client';
vi.mock('./client', async (load) => ({
  ...(await load<typeof import('./client')>()),
  platformRequest: vi.fn(),
  command: vi.fn(),
}));
describe('Teaching workspace', () => {
  it('creates a multi-session template with ordered lesson steps', async () => {
    vi.mocked(client.platformRequest).mockResolvedValue([]);
    vi.mocked(client.command).mockResolvedValue({ id: '1' });
    render(
      <QueryClientProvider
        client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
      >
        <TeachingPage />
      </QueryClientProvider>,
    );
    await userEvent.click(screen.getByRole('button', { name: '新建主题' }));
    await userEvent.type(screen.getByLabelText('主题名称'), '森林里的房子');
    await userEvent.type(screen.getByLabelText('教学目标'), '理解空间与色彩');
    await userEvent.type(screen.getByLabelText('材料清单'), '纸张、水彩');
    await userEvent.type(screen.getByLabelText('第 1 课次内容'), '观察与构图');
    await userEvent.click(screen.getByRole('button', { name: '保存主题草稿' }));
    await waitFor(() =>
      expect(client.command).toHaveBeenCalledWith(
        '/teaching/templates',
        expect.objectContaining({
          title: '森林里的房子',
          expectedLessons: 3,
          steps: expect.arrayContaining([expect.objectContaining({ content: '观察与构图' })]),
        }),
        'POST',
      ),
    );
  });
});

it('copies public teaching media through the authorized source endpoint before editing', async () => {
  const content = {
    title: '公开水彩主题',
    ageMin: 4,
    ageMax: 8,
    expectedLessons: 3,
    goals: '颜色观察',
    materials: '水彩',
    steps: [],
    tags: [],
    mediaIds: ['private-source-media'],
  };
  vi.mocked(client.platformRequest).mockImplementation(async (path) =>
    path.startsWith('/teaching/template-versions')
      ? [
          {
            id: 'pub',
            templateId: 'source',
            version: 2,
            content,
            createdAt: '2026-09-08T00:00:00Z',
          },
        ]
      : [],
  );
  vi.mocked(client.command).mockClear().mockResolvedValue({ id: 'copy', version: 0, content });
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <TeachingPage />
    </QueryClientProvider>,
  );
  await userEvent.click(await screen.findByRole('button', { name: '复制此发布版本' }));
  await waitFor(() =>
    expect(client.command).toHaveBeenCalledWith('/teaching/template-versions/pub/copy', {}),
  );
  await userEvent.click(await screen.findByRole('button', { name: '保存主题草稿' }));
  await waitFor(() =>
    expect(client.command).toHaveBeenCalledWith(
      '/teaching/templates/copy',
      expect.objectContaining({ mediaIds: ['private-source-media'], version: 0 }),
      'PUT',
    ),
  );
  expect(client.command).not.toHaveBeenCalledWith('/teaching/templates', expect.anything(), 'POST');
});
