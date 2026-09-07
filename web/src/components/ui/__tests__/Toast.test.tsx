import { describe, expect, it, vi } from 'vitest';
import * as Sonner from 'sonner';
import { toast } from '@/lib/toast';

describe('Toast', () => {
  it('delegates success to sonner', () => {
    const spy = vi.spyOn(Sonner.toast, 'success').mockImplementation(() => 1);
    toast.success('已保存');
    expect(spy).toHaveBeenCalledWith('已保存');
    spy.mockRestore();
  });

  it('undo invokes without throwing', () => {
    const onUndo = vi.fn();
    expect(() => toast.undo({ message: '已删除', onUndo })).not.toThrow();
  });
});
