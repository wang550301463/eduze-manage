import { describe, it, expect, vi } from 'vitest';
import { AudioCapture } from '../../../../miniapp/core/audio';
describe('original audio capture', () => {
  it('discards recordings stopped after role changes', () => {
    let stop!: (file: { tempFilePath: string; fileSize: number }) => void;
    let scope = 1;
    const received = vi.fn();
    const manager = {
      start: vi.fn(),
      stop: vi.fn(),
      onStop: (fn: typeof stop) => {
        stop = fn;
      },
      offStop: vi.fn(),
      onError: vi.fn(),
      offError: vi.fn(),
    };
    const capture = new AudioCapture(manager, () => scope, received, vi.fn());
    capture.start();
    scope = 2;
    stop({ tempFilePath: 'private.mp3', fileSize: 20 });
    expect(received).not.toHaveBeenCalled();
    capture.dispose();
    expect(manager.offStop).toHaveBeenCalled();
  });
  it('records a bounded raw mp3 and retains failed upload inputs for retry by the page', () => {
    let stop!: (file: { tempFilePath: string; fileSize: number }) => void;
    const received = vi.fn();
    const manager = {
      start: vi.fn(),
      stop: vi.fn(),
      onStop: (fn: typeof stop) => {
        stop = fn;
      },
      offStop: vi.fn(),
      onError: vi.fn(),
      offError: vi.fn(),
    };
    const capture = new AudioCapture(manager, () => 1, received, vi.fn());
    capture.start();
    expect(manager.start).toHaveBeenCalledWith({ format: 'mp3', duration: 60000 });
    stop({ tempFilePath: 'voice.mp3', fileSize: 99 });
    expect(received).toHaveBeenCalledWith({ tempFilePath: 'voice.mp3', fileSize: 99 });
  });
});
