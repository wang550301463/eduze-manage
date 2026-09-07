import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ColumnDef } from '@tanstack/react-table';
import { describe, expect, it, vi } from 'vitest';
import { DataTable } from '../DataTable';

type Row = { id: string; name: string };

const columns: ColumnDef<Row>[] = [
  { accessorKey: 'name', header: '姓名' },
];

const data: Row[] = Array.from({ length: 5 }, (_, i) => ({
  id: String(i + 1),
  name: `学员${i + 1}`,
}));

describe('DataTable', () => {
  it('renders 5 rows', () => {
    render(
      <DataTable columns={columns} data={data} rowKey={(r) => r.id} />,
    );
    const table = screen.getByRole('table');
    expect(within(table).getAllByText(/学员\d/)).toHaveLength(5);
  });

  it('calls onRowClick', async () => {
    const user = userEvent.setup();
    const onRowClick = vi.fn();
    render(
      <DataTable
        columns={columns}
        data={data}
        rowKey={(r) => r.id}
        onRowClick={onRowClick}
      />,
    );
    const table = screen.getByRole('table');
    await user.click(within(table).getByText('学员1'));
    expect(onRowClick).toHaveBeenCalledWith(data[0]);
  });

  it('shows skeleton when loading', () => {
    const { container } = render(
      <DataTable columns={columns} data={[]} loading rowKey={(r) => r.id} />,
    );
    expect(container.querySelectorAll('.animate-pulse').length).toBeGreaterThan(0);
  });

  it('renders mobile cards at narrow width', () => {
    const original = window.innerWidth;
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 375 });
    window.dispatchEvent(new Event('resize'));
    render(
      <DataTable
        columns={columns}
        data={data}
        rowKey={(r) => r.id}
        mobileCardRender={(row) => <span data-testid="card">{row.name}</span>}
      />,
    );
    expect(screen.getAllByTestId('card')).toHaveLength(5);
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: original });
  });
});
