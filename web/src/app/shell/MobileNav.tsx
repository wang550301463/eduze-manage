import { NavLink } from 'react-router-dom';
import { RequirePermission } from '@/components/auth/RequirePermission';
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/Sheet';
import { allNavItems, navItemEnd } from '@/app/shell/nav-config';
import { useShell } from '@/app/shell/shell-context';
import { useAuthStore } from '@/features/auth/store';
import { prefersTeachingHome } from '@/features/dashboard/home-role';
import { cn } from '@/lib/cn';

export function MobileNav(): JSX.Element {
  const user = useAuthStore((s) => s.user);
  const roles = user?.roles ?? [];
  const { mobileNavOpen, setMobileNavOpen } = useShell();

  return (
    <Sheet open={mobileNavOpen} onOpenChange={setMobileNavOpen}>
      <SheetContent side="left" className="w-[min(100%,280px)] p-0 sm:max-w-xs">
        <SheetHeader className="shrink-0 border-b border-border px-4 py-4">
          <SheetTitle className="text-foreground">美术宝 · 工作空间</SheetTitle>
        </SheetHeader>
        <nav
          className="flex min-h-0 flex-1 flex-col gap-1 overflow-y-auto p-2"
          aria-label="移动端导航"
        >
          {allNavItems
            .filter(
              (item) =>
                item.path !== '/workbench' ||
                (roles.includes('TEACHER') && !prefersTeachingHome(roles)),
            )
            .map((item) => (
              <RequirePermission key={item.path} perm={item.permission}>
                <NavLink
                  to={item.path}
                  end={navItemEnd(item.path)}
                  onClick={() => setMobileNavOpen(false)}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-3 rounded-[10px] px-3 py-3 text-sm transition-colors',
                      isActive ? 'studio-nav-active' : 'text-foreground/80 hover:bg-muted',
                    )
                  }
                >
                  {({ isActive }) => (
                    <>
                      <item.icon className="h-5 w-5" weight={isActive ? 'fill' : 'regular'} />
                      <span>{item.label}</span>
                    </>
                  )}
                </NavLink>
              </RequirePermission>
            ))}
        </nav>
      </SheetContent>
    </Sheet>
  );
}
