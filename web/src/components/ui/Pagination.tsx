import { CaretLeft, CaretRight } from '@phosphor-icons/react';
import { cn } from '@/lib/cn';
import { Button } from './Button';

export type PaginationProps = {
  page: number;
  pageSize: number;
  total: number;
  onChange: (next: { page: number; pageSize: number }) => void;
  className?: string;
};

function getPageNumbers(current: number, totalPages: number): (number | 'ellipsis')[] {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, i) => i + 1);
  }
  const pages: (number | 'ellipsis')[] = [1];
  if (current > 3) pages.push('ellipsis');
  for (let i = Math.max(2, current - 1); i <= Math.min(totalPages - 1, current + 1); i++) {
    pages.push(i);
  }
  if (current < totalPages - 2) pages.push('ellipsis');
  pages.push(totalPages);
  return pages;
}

export function Pagination({
  page,
  pageSize,
  total,
  onChange,
  className,
}: PaginationProps): JSX.Element {
  const totalPages = Math.max(1, Math.ceil(total / pageSize));

  const goTo = (nextPage: number): void => {
    onChange({ page: Math.min(Math.max(1, nextPage), totalPages), pageSize });
  };

  return (
  <>
      <nav
        className={cn('hidden items-center gap-1 md:flex', className)}
        aria-label="分页"
      >
        <Button
          variant="ghost"
          size="sm"
          aria-label="上一页"
          disabled={page <= 1}
          onClick={() => goTo(page - 1)}
        >
          <CaretLeft className="h-4 w-4" />
        </Button>
        {getPageNumbers(page, totalPages).map((p, idx) =>
          p === 'ellipsis' ? (
            <span key={`e-${idx}`} className="px-2 text-muted-fg">
              …
            </span>
          ) : (
            <Button
              key={p}
              variant={p === page ? 'default' : 'ghost'}
              size="sm"
              aria-current={p === page ? 'page' : undefined}
              onClick={() => goTo(p)}
            >
              {p}
            </Button>
          ),
        )}
        <Button
          variant="ghost"
          size="sm"
          aria-label="下一页"
          disabled={page >= totalPages}
          onClick={() => goTo(page + 1)}
        >
          <CaretRight className="h-4 w-4" />
        </Button>
      </nav>
      <div className={cn('flex items-center justify-center gap-2 md:hidden', className)}>
        <Button
          variant="ghost"
          size="sm"
          aria-label="上一页"
          disabled={page <= 1}
          onClick={() => goTo(page - 1)}
        >
          <CaretLeft className="h-4 w-4" />
        </Button>
        <span className="text-sm text-muted-fg">
          {page} / {totalPages}
        </span>
        <Button
          variant="ghost"
          size="sm"
          aria-label="下一页"
          disabled={page >= totalPages}
          onClick={() => goTo(page + 1)}
        >
          <CaretRight className="h-4 w-4" />
        </Button>
      </div>
    </>
  );
}
