import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, it, expect, vi } from 'vitest';
import { LeadEditor } from './EngagementPage';
import { ProductEditor } from './CommercePage';
import { parsePrice } from '../../../../packages/contracts/business';
import * as client from './client';
vi.mock('@/features/lesson/api', () => ({
  fetchWeekSchedule: vi.fn().mockResolvedValue({ days: [] }),
}));
vi.mock('@/features/course/api', () => ({
  listClassGroups: vi.fn().mockResolvedValue({ items: [] }),
  listCourses: vi.fn().mockResolvedValue({ items: [] }),
}));
vi.mock('./client', async (load) => ({
  ...(await load<typeof import('./client')>()),
  command: vi.fn(),
  platformRequest: vi.fn(),
}));
describe('catalog price and fulfillment boundaries', () => {
  it('parses currency without losing cents and rejects more than two decimal places', () => {
    expect(parsePrice('0.29')).toBe(29);
    expect(() => parsePrice('12.999')).toThrow('两位');
    expect(() => parsePrice('-1')).toThrow();
  });
  it('creates a pickup-capable physical item with money in integer cents', async () => {
    vi.mocked(client.command).mockResolvedValue({});
    render(
      <QueryClientProvider client={new QueryClient()}>
        <ProductEditor branchId="b" onSaved={() => {}} />
      </QueryClientProvider>,
    );
    await userEvent.type(screen.getByLabelText('商品名称'), '水彩材料包');
    await userEvent.type(screen.getByLabelText('商品介绍'), '课堂配套材料');
    await userEvent.type(screen.getByLabelText('售价（元）'), '29.90');
    await userEvent.click(screen.getByRole('button', { name: '保存商品' }));
    await waitFor(() =>
      expect(client.command).toHaveBeenCalledWith(
        '/commerce/products',
        expect.objectContaining({
          type: 'PHYSICAL',
          title: '水彩材料包',
          priceMinor: 2990,
          branchId: 'b',
        }),
      ),
    );
  });
});

it('preserves enrolled state and disables another trial reservation', () => {
  vi.mocked(client.platformRequest).mockResolvedValue([]);
  render(
    <QueryClientProvider client={new QueryClient()}>
      <LeadEditor
        enquiry={{
          id: 'e',
          branchId: 'b',
          studentName: '小明',
          phone: '123',
          age: 7,
          source: '',
          sourceId: '',
          status: 'ENROLLED',
          assigneeId: '',
          reservationId: 'r',
        }}
        onClose={() => {}}
      />
    </QueryClientProvider>,
  );
  expect(screen.getByLabelText('跟进状态')).toHaveValue('ENROLLED');
  expect(screen.getByLabelText('跟进状态')).toBeDisabled();
  expect(screen.getByRole('button', { name: '提交教务确认名额' })).toBeDisabled();
  expect(screen.queryByRole('button', { name: '确认加入所选班级' })).not.toBeInTheDocument();
});
it('submits the loaded inventory version when editing a product', async () => {
  vi.mocked(client.command).mockClear().mockResolvedValue({});
  render(
    <QueryClientProvider client={new QueryClient()}>
      <ProductEditor
        branchId="b"
        product={{
          id: 'p',
          version: 8,
          branchId: 'b',
          type: 'PHYSICAL',
          title: '画纸',
          description: '水彩画纸',
          priceMinor: 100,
          stock: 3,
          lessonUnits: 0,
          published: true,
        }}
        onSaved={() => {}}
      />
    </QueryClientProvider>,
  );
  await userEvent.click(screen.getByRole('button', { name: '保存商品' }));
  await waitFor(() =>
    expect(client.command).toHaveBeenCalledWith(
      '/commerce/products',
      expect.objectContaining({ id: 'p', version: 8, stock: 3 }),
    ),
  );
});
