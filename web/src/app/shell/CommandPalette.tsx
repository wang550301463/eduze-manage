import { Command } from 'cmdk';
import { Question } from '@phosphor-icons/react';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useShell } from '@/app/shell/shell-context';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { Skeleton } from '@/components/ui/Skeleton';
import { cn } from '@/lib/cn';
import { openSearchHit } from '@/lib/openEntity';
import {
  search,
  searchGroupLabels,
  type SearchResult,
  type SearchResultGroup,
} from '@/lib/searchClient';

const GROUP_ORDER: SearchResultGroup[] = ['student', 'guardian', 'class', 'lesson'];

export function CommandPalette(): JSX.Element {
  const { paletteOpen, setPaletteOpen, setHelpOpen } = useShell();
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<SearchResult[]>([]);
  const [composing, setComposing] = useState(false);

  const runSearch = useCallback(async (q: string) => {
    if (!q.trim()) {
      setResults([]);
      return;
    }
    setLoading(true);
    try {
      const res = await search(q);
      setResults(res.results);
    } catch {
      setResults([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!paletteOpen) {
      setQuery('');
      setResults([]);
      return;
    }
    if (composing) {
      return;
    }
    const timer = window.setTimeout(() => {
      void runSearch(query);
    }, 300);
    return () => window.clearTimeout(timer);
  }, [query, paletteOpen, composing, runSearch]);

  const grouped = GROUP_ORDER.map((group) => ({
    group,
    items: results.filter((r) => r.group === group),
  })).filter((g) => g.items.length > 0);

  const handleSelect = (item: SearchResult) => {
    setPaletteOpen(false);
    openSearchHit(item, navigate);
  };

  return (
    <Dialog open={paletteOpen} onOpenChange={setPaletteOpen}>
      <DialogContent className="max-w-xl gap-0 overflow-hidden p-0">
        <Command className="flex flex-col" shouldFilter={false} loop label="全局搜索">
          <div className="flex items-center border-b border-border px-3" role="search">
            <Command.Input
              value={query}
              onValueChange={setQuery}
              onCompositionStart={() => setComposing(true)}
              onCompositionEnd={() => setComposing(false)}
              placeholder="搜索学员、家长、班级、课次…"
              className="flex h-12 flex-1 bg-transparent py-3 text-sm outline-none placeholder:text-muted-fg"
              autoFocus
              data-testid="command-palette-input"
            />
            <button
              type="button"
              className="rounded-md p-2 text-muted-fg hover:bg-muted hover:text-foreground"
              aria-label="快捷键帮助"
              onClick={() => {
                setPaletteOpen(false);
                setHelpOpen(true);
              }}
            >
              <Question className="h-4 w-4" weight="bold" />
            </button>
          </div>
          <Command.List
            className="max-h-[min(60vh,420px)] overflow-y-auto p-2"
            data-testid="command-palette-list"
          >
            {loading ? (
              <div className="space-y-2 p-2" data-testid="command-palette-loading">
                <Skeleton className="h-8 w-full" />
                <Skeleton className="h-8 w-3/4" />
                <Skeleton className="h-8 w-2/3" />
              </div>
            ) : null}
            {!loading && query.trim() && grouped.length === 0 ? (
              <Command.Empty className="py-8 text-center text-sm text-muted-fg">
                未找到相关结果
              </Command.Empty>
            ) : null}
            {!loading &&
              grouped.map(({ group, items }) => (
                <Command.Group key={group} heading={searchGroupLabels[group]}>
                  {items.map((item) => (
                    <Command.Item
                      key={item.id}
                      value={item.id}
                      onSelect={() => handleSelect(item)}
                      data-testid={`search-hit-${item.group}-${item.entityId}`}
                      className={cn(
                        'flex cursor-pointer flex-col rounded-md px-3 py-2 text-sm',
                        'aria-selected:bg-primary/10',
                      )}
                    >
                      <span className="font-medium">{item.title}</span>
                      {item.subtitle ? (
                        <span className="text-xs text-muted-fg">{item.subtitle}</span>
                      ) : null}
                    </Command.Item>
                  ))}
                </Command.Group>
              ))}
            {!query.trim() ? (
              <p className="px-3 py-6 text-center text-sm text-muted-fg">
                输入关键词开始搜索，↑↓ 选择，Enter 打开
              </p>
            ) : null}
          </Command.List>
        </Command>
      </DialogContent>
    </Dialog>
  );
}

