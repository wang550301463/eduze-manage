import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Button } from '../Button';

describe('Button', () => {
  it('renders default variant', () => {
    const { container } = render(<Button>主按钮</Button>);
    expect(screen.getByRole('button', { name: '主按钮' })).toBeInTheDocument();
    expect(container).toMatchSnapshot();
  });

  it('renders danger variant', () => {
    const { container } = render(<Button variant="danger">删除</Button>);
    expect(container).toMatchSnapshot();
  });

  it('renders sm size', () => {
    const { container } = render(
      <Button variant="default" size="sm">
        小按钮
      </Button>,
    );
    expect(container).toMatchSnapshot();
  });
});
