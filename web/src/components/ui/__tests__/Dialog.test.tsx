import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { Button } from '../Button';
import { DialogFrame } from '../Dialog';

describe('Dialog', () => {
  it('opens on trigger click', async () => {
    const user = userEvent.setup();
    render(
      <DialogFrame trigger={<Button>打开</Button>} title="标题" description="描述">
        内容
      </DialogFrame>,
    );
    await user.click(screen.getByRole('button', { name: '打开' }));
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('标题')).toBeInTheDocument();
  });
});
