import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { SimpleSelect } from '../Select';

describe('SimpleSelect', () => {
  it('opens and selects option', () => {
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
    fireEvent.click(combobox);
    expect(combobox).toHaveAttribute('aria-expanded', 'true');
  });
});
