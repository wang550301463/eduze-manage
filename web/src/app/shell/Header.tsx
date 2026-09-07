import { Bell, List, MagnifyingGlass } from '@phosphor-icons/react';
import { useLocation } from 'react-router-dom';
import { getNavLabel } from '@/app/shell/nav-config';
import { Button } from '@/components/ui/Button';
import { UserMenu } from '@/features/auth/components/UserMenu';
import { cn } from '@/lib/cn';

type HeaderProps = {
  onOpenSearch?: () => void;
  onOpenMobileNav?: () => void;
};

export function Header({ onOpenSearch, onOpenMobileNav }: HeaderProps): JSX.Element {
  const { pathname } = useLocation();
  const breadcrumb = getNavLabel(pathname);

  return (
    <header className="sticky top-0 z-30 flex h-14 shrink-0 items-center gap-3 border-b border-border bg-background/80 px-4 backdrop-blur lg:grid lg:grid-cols-[1fr_auto]">
      <div className="flex min-w-0 flex-1 items-center gap-3">
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="shrink-0 lg:hidden"
          onClick={onOpenMobileNav}
          aria-label="打开导航菜单"
        >
          <List className="h-5 w-5" />
        </Button>
        <span className="absolute left-1/2 -translate-x-1/2 font-serif text-lg font-bold text-primary lg:static lg:translate-x-0">
          EduZE Manage
        </span>
        <nav aria-label="面包屑" className="ml-auto hidden text-sm text-muted-fg lg:ml-4 lg:block">
          <span>{breadcrumb}</span>
        </nav>
      </div>

      <div className="flex items-center gap-2">
        <Button
          type="button"
          variant="secondary"
          size="sm"
          className={cn('gap-2')}
          onClick={onOpenSearch}
          aria-label="全局搜索"
        >
          <MagnifyingGlass className="h-4 w-4" />
          <span className="hidden text-muted-fg sm:inline">搜索…</span>
          <kbd className="ml-1 hidden rounded border border-border bg-muted px-1.5 py-0.5 font-mono text-xs md:inline">
            ⌘K
          </kbd>
        </Button>
        <Button type="button" variant="ghost" size="sm" aria-label="消息通知（占位）">
          <Bell className="h-5 w-5" />
        </Button>
        <UserMenu />
      </div>
    </header>
  );
}
