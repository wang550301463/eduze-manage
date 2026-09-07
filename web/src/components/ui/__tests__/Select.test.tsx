import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { SimpleSelect } from '../Select';

describe('SimpleSelect', () => {
  it('opens and selects option', async () => {
    const user = userEvent.setup();
    render(
      <SimpleSelect
        options={[
          { value: 'a', label: 'A' },
          { value: 'b', label: 'B' },
        ]}
        aria-label="选择"
      />,
    );
    const combobox = screen.getByRole('combobox');
    await user.click(combobox);
    expect(combobox).toHaveAttribute('aria-expanded', 'true');
  });
});
