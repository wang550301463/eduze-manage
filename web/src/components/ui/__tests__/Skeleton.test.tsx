import { render } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Skeleton } from '../Skeleton';

describe('Skeleton', () => {
  it('renders with custom class', () => {
    const { container } = render(<Skeleton className="h-8 w-32" />);
    expect(container.firstChild).toHaveClass('h-8', 'w-32', 'animate-pulse');
  });
});
