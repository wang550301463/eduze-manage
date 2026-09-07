import { NavLink } from 'react-router-dom';
import { RequirePermission } from '@/components/auth/RequirePermission';
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/Sheet';
import { allNavItems, navItemEnd } from '@/app/shell/nav-config';
import { useShell } from '@/app/shell/shell-context';
import { cn } from '@/lib/cn';

export function MobileNav(): JSX.Element {
  const { mobileNavOpen, setMobileNavOpen } = useShell();

  return (
    <Sheet open={mobileNavOpen} onOpenChange={setMobileNavOpen}>
      <SheetContent side="left" className="w-[min(100%,280px)] p-0 sm:max-w-xs">
        <SheetHeader className="border-b border-border px-4 py-4">
          <SheetTitle className="font-serif text-primary">EduZE Manage</SheetTitle>
        </SheetHeader>
        <nav className="flex flex-col gap-1 p-2" aria-label="移动端导航">
          {allNavItems.map((item) => (
            <RequirePermission key={item.path} perm={item.permission}>
              <NavLink
                to={item.path}
                end={navItemEnd(item.path)}
                onClick={() => setMobileNavOpen(false)}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 rounded-[10px] px-3 py-2 text-sm transition-colors',
                    isActive
                      ? 'bg-primary font-medium text-primary-fg'
                      : 'text-foreground/80 hover:bg-muted',
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
