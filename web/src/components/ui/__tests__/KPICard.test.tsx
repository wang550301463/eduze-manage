import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { KPICard } from '../KPICard';

describe('KPICard', () => {
  it('renders animated value', async () => {
    render(<KPICard title="学员数" value={2500} />);
    expect(screen.getByText('学员数')).toBeInTheDocument();
    await waitFor(
      () => {
        expect(screen.getByText(/2,?500/)).toBeInTheDocument();
      },
      { timeout: 1500 },
    );
  });
});
