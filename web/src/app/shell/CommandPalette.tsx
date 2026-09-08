import { Command } from 'cmdk';
import { MagnifyingGlass, Question } from '@phosphor-icons/react';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useShell } from '@/app/shell/shell-context';
import { useAuthStore } from '@/features/auth/store';
import { Dialog, DialogContent, DialogTitle, DialogDescription } from '@/components/ui/Dialog';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { cn } from '@/lib/cn';
import { openSearchHit, searchHitUrl } from '@/lib/openEntity';
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
  const permissions = useAuthStore((state) => state.permissions);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<SearchResult[]>([]);
  const [composing, setComposing] = useState(false);
  const [error, setError] = useState(false);
  const [retry, setRetry] = useState(0);
  const [guardian, setGuardian] = useState<SearchResult | null>(null);

  useEffect(() => {
    setResults([]);
    setError(false);
    setGuardian(null);
    if (!paletteOpen) {
      setQuery('');
      setLoading(false);
      setComposing(false);
      return;
    }
    if (composing || !query.trim()) {
      setLoading(false);
      return;
    }
    const controller = new AbortController();
    setLoading(true);
    const timer = window.setTimeout(() => {
      void search(query, 20, controller.signal)
        .then((res) => {
          if (!controller.signal.aborted) setResults(res.results);
        })
        .catch(() => {
          if (!controller.signal.aborted) setError(true);
        })
        .finally(() => {
          if (!controller.signal.aborted) setLoading(false);
        });
    }, 300);
    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [query, paletteOpen, composing, retry]);

  const visibleResults = results.filter((item) => {
    switch (item.group) {
      case 'student':
        return permissions.includes('student:read');
      case 'class':
        return permissions.includes('classgroup:read');
      case 'lesson':
        return permissions.includes('lesson:read');
      case 'guardian':
        return (
          permissions.includes('guardian:read') &&
          (!searchHitUrl(item) || permissions.includes('student:read'))
        );
    }
  });
  const grouped = GROUP_ORDER.map((group) => ({
    group,
    items: visibleResults.filter((r) => r.group === group),
  })).filter((g) => g.items.length > 0);

  const handleSelect = (item: SearchResult) => {
    if (item.group === 'guardian' && !searchHitUrl(item)) {
      setGuardian(item);
      return;
    }
    setPaletteOpen(false);
    openSearchHit(item, navigate);
  };

  return (
    <Dialog open={paletteOpen} onOpenChange={setPaletteOpen}>
      <DialogContent className="command-palette max-w-xl gap-0 overflow-hidden p-0 focus-within:ring-2 focus-within:ring-primary/20 focus-within:ring-offset-0">
        <DialogTitle className="sr-only">全局搜索</DialogTitle>
        <DialogDescription className="sr-only">
          搜索学员、家长、班级和课次，用方向键选择并按回车打开。
        </DialogDescription>
        <Command className="flex flex-col" shouldFilter={false} loop label="全局搜索">
          <div
            className="flex h-16 items-center gap-3 border-b border-border/60 pl-5 pr-16"
            role="search"
          >
            <MagnifyingGlass className="h-5 w-5 shrink-0 text-muted-fg" aria-hidden />
            <Command.Input
              value={query}
              onValueChange={setQuery}
              onCompositionStart={() => setComposing(true)}
              onCompositionEnd={() => setComposing(false)}
              placeholder="搜索学员、家长、班级、课次…"
              className="h-full min-w-0 flex-1 border-0 bg-transparent p-0 text-base leading-normal outline-none placeholder:text-muted-fg/80 focus-visible:outline-none focus-visible:ring-0 focus-visible:ring-offset-0"
              autoFocus
              data-testid="command-palette-input"
            />
          </div>
          <Command.List
            className="max-h-[min(60vh,420px)] overflow-y-auto p-2"
            data-testid="command-palette-list"
          >
            {loading ? (
              <div
                role="status"
                aria-label="正在搜索"
                className="space-y-2 p-2"
                data-testid="command-palette-loading"
              >
                <Skeleton className="h-8 w-full" />
                <Skeleton className="h-8 w-3/4" />
                <Skeleton className="h-8 w-2/3" />
              </div>
            ) : null}
            {error ? (
              <div role="alert" className="space-y-3 px-3 py-6 text-center text-sm text-muted-fg">
                <p>搜索暂时不可用，请稍后重试</p>
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => setRetry((value) => value + 1)}
                >
                  重试搜索
                </Button>
              </div>
            ) : null}
            {!loading && !error && !composing && query.trim() && grouped.length === 0 ? (
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
                        'flex cursor-pointer flex-col items-start gap-1 rounded-xl px-3 py-3 text-sm',
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
              <p className="px-3 py-5 text-center text-sm text-muted-fg">
                输入姓名、编号或课程关键词
              </p>
            ) : null}
          </Command.List>
          {guardian ? (
            <section
              aria-label="家长联系信息"
              className="space-y-2 border-t border-border bg-muted/40 px-5 py-4 text-sm"
            >
              <p className="font-semibold">{guardian.title}</p>
              <p>{guardian.subtitle}</p>
              <p className="text-xs text-muted-fg">暂无可打开的关联学员记录</p>
            </section>
          ) : null}
          <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1 border-t border-border/50 bg-muted/25 px-4 py-2 text-[11px] text-muted-fg">
            <span>↑↓ 选择 · ↵ 打开 · Esc 关闭</span>
            <button
              type="button"
              className="flex items-center gap-1.5 rounded-lg px-2 py-1.5 transition-colors hover:bg-muted hover:text-foreground"
              aria-label="快捷键帮助"
              onClick={() => {
                setPaletteOpen(false);
                setHelpOpen(true);
              }}
            >
              <Question className="h-4 w-4" aria-hidden />
              快捷键
            </button>
          </div>
        </Command>
      </DialogContent>
    </Dialog>
  );
}
