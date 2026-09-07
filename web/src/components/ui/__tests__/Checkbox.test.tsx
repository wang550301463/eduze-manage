import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Checkbox } from '../Checkbox';

describe('Checkbox', () => {
  it('toggles on click', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Checkbox aria-label="同意" onCheckedChange={onChange} />);
    await user.click(screen.getByRole('checkbox'));
    expect(onChange).toHaveBeenCalledWith(true);
  });
});
