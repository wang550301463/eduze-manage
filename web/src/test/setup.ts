import '@testing-library/jest-dom/vitest';
import { vi } from 'vitest';

// Radix Select / pointer events in jsdom
if (!Element.prototype.hasPointerCapture) {
  Element.prototype.hasPointerCapture = () => false;
}
if (!Element.prototype.setPointerCapture) {
  Element.prototype.setPointerCapture = () => undefined;
}
if (!Element.prototype.releasePointerCapture) {
  Element.prototype.releasePointerCapture = () => undefined;
}
if (!Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = () => undefined;
}

// Tailwind `md:` breakpoint — default desktop table layout in unit tests
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: query.includes('min-width: 768px'),
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});
