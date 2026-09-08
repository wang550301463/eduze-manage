import { readFileSync, readdirSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, it, expect } from 'vitest';
describe('native page bindings', () => {
  it('uses evaluated WXML conditions and ships every registered page', () => {
    const root = resolve(process.cwd(), '../miniapp');
    const app = JSON.parse(readFileSync(resolve(root, 'app.json'), 'utf8')) as { pages: string[] };
    for (const page of app.pages) {
      const markup = readFileSync(resolve(root, `${page}.wxml`), 'utf8');
      expect(readFileSync(resolve(root, `${page}.ts`), 'utf8')).toContain('Page(');
      for (const [, expression] of markup.matchAll(/wx:(?:if|elif)="([^"]+)"/g))
        expect(expression, `${page}: ${expression}`).toMatch(/^\{\{.*\}\}$/);
    }
    expect(readdirSync(resolve(root, 'pages'))).toContain('orders');
  });
});
