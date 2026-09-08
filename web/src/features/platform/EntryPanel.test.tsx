import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, it, expect, vi } from 'vitest';
import { EntryPanel } from './EntryPanel';
import * as client from './client';
vi.mock('./client', async (load) => ({
  ...(await load<typeof import('./client')>()),
  platformRequest: vi.fn(),
  command: vi.fn(),
}));
describe('per-session classroom dynamics', () => {
  it('stores classroom notes as a draft independently of final portfolio publication', async () => {
    vi.mocked(client.platformRequest).mockImplementation(async (path) =>
      path.includes('/teaching/') ? { content: { lessonIds: ['lesson-3'] } } : [],
    );
    vi.mocked(client.command).mockResolvedValue({});
    render(
      <QueryClientProvider client={new QueryClient()}>
        <EntryPanel recordId="r" themeId="t" branchId="b" />
      </QueryClientProvider>,
    );
    await screen.findByRole('option', { name: '第 1 次关联课堂' });
    await userEvent.selectOptions(screen.getByLabelText('对应课堂'), 'lesson-3');
    await userEvent.type(screen.getByLabelText('本次课堂观察'), '今天完成了构图');
    await userEvent.click(screen.getByRole('button', { name: '保存本次课堂草稿' }));
    await waitFor(() =>
      expect(client.command).toHaveBeenCalledWith(
        '/portfolio/records/r/entries',
        expect.objectContaining({
          lessonId: 'lesson-3',
          notes: '今天完成了构图',
          idempotencyKey: expect.any(String),
        }),
      ),
    );
    expect(vi.mocked(client.command).mock.calls.some(([path]) => path.endsWith('/publish'))).toBe(
      false,
    );
  });
});
