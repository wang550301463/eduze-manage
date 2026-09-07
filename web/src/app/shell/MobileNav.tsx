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
                    'flex items-center gap-3 rounded-md px-3 py-2.5 text-sm',
                    isActive
                      ? 'bg-primary/10 font-medium text-primary'
                      : 'text-muted-fg hover:bg-muted',
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
