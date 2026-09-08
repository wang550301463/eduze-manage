import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, it, expect, vi } from 'vitest';
import { PortfolioEditor } from './PortfolioPage';
import * as client from './client';
vi.mock('./client', async (load) => ({
  ...(await load<typeof import('./client')>()),
  command: vi.fn(),
  platformRequest: vi.fn(),
}));
describe('portfolio publishing', () => {
  it('requires a saved preview before publication and preserves the concurrency version', async () => {
    vi.mocked(client.command).mockResolvedValue({ id: 'publication' });
    vi.mocked(client.platformRequest).mockImplementation(async (path) =>
      path.includes('/teaching/')
        ? { content: { lessonIds: [] } }
        : path.endsWith('/entries')
          ? []
          : {},
    );
    render(
      <QueryClientProvider client={new QueryClient()}>
        <PortfolioEditor
          record={{
            id: 'r1',
            version: 7,
            branchId: 'b',
            status: 'DRAFT',
            createdAt: '',
            content: {
              themeId: 't',
              studentId: 's',
              progress: 'COMPLETED',
              classroomNote: '共同构图',
              comment: '大胆表达',
              artworks: [],
              audioMediaIds: [],
            },
          }}
          roster={[]}
          onSaved={() => {}}
        />
      </QueryClientProvider>,
    );
    expect(screen.queryByRole('button', { name: '确认发布给家庭' })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: '预览课效' }));
    expect(screen.getByText('大胆表达', { selector: 'p' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: '确认发布给家庭' }));
    expect(client.command).toHaveBeenCalledWith(
      '/portfolio/records/r1/publish',
      expect.objectContaining({ version: 7, idempotencyKey: expect.any(String) }),
    );
  });
});
