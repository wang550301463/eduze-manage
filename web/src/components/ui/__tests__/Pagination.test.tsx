import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Pagination } from '../Pagination';

describe('Pagination', () => {
  it('changes page on click', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(
      <Pagination page={1} pageSize={10} total={50} onChange={onChange} />,
    );
    const page2 = screen.getAllByRole('button', { name: '2' })[0];
    await user.click(page2);
    expect(onChange).toHaveBeenCalledWith({ page: 2, pageSize: 10 });
  });
});
