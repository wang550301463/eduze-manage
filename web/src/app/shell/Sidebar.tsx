import { CaretLeft, CaretRight } from '@phosphor-icons/react';
import { NavLink } from 'react-router-dom';
import { RequirePermission } from '@/components/auth/RequirePermission';
import { Button } from '@/components/ui/Button';
import { mainNavItems, settingsNavItems } from '@/app/shell/nav-config';
import { useAuthStore } from '@/features/auth/store';
import { cn } from '@/lib/cn';

type SidebarProps = {
  className?: string;
};

function NavSection({
  title,
  collapsed,
}: {
  title: string;
  collapsed: boolean;
}): JSX.Element | null {
  if (collapsed) return null;
  return (
    <p className="mb-2 px-3 text-xs font-medium uppercase tracking-wide text-muted-fg">{title}</p>
  );
}

export function Sidebar({ className }: SidebarProps): JSX.Element {
  const collapsed = useAuthStore((s) => s.sidebarCollapsed);
  const setCollapsed = useAuthStore((s) => s.setSidebarCollapsed);

  return (
    <aside
      className={cn(
        'hidden h-full shrink-0 flex-col border-r border-border bg-background lg:flex',
        collapsed ? 'w-16' : 'w-60',
        className,
      )}
      aria-label="主导航"
    >
      <nav className="flex flex-1 flex-col gap-1 overflow-y-auto p-2">
        {mainNavItems.map((item) => (
          <RequirePermission key={item.path} perm={item.permission}>
            <NavLink
              to={item.path}
              end={item.path === '/'}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors',
                  isActive
                    ? 'bg-primary/10 font-medium text-primary'
                    : 'text-muted-fg hover:bg-muted hover:text-foreground',
                )
              }
              aria-current={undefined}
            >
              {({ isActive }) => (
                <>
                  <item.icon className="h-5 w-5 shrink-0" weight={isActive ? 'fill' : 'regular'} />
                  {!collapsed ? <span>{item.label}</span> : null}
                </>
              )}
            </NavLink>
          </RequirePermission>
        ))}

        <div className="my-2 border-t border-border" />
        <NavSection title="系统" collapsed={collapsed} />
        {settingsNavItems.map((item) => (
          <RequirePermission key={item.path} perm={item.permission}>
            <NavLink
              to={item.path}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors',
                  isActive
                    ? 'bg-primary/10 font-medium text-primary'
                    : 'text-muted-fg hover:bg-muted hover:text-foreground',
                )
              }
            >
              {({ isActive }) => (
                <>
                  <item.icon className="h-5 w-5 shrink-0" weight={isActive ? 'fill' : 'regular'} />
                  {!collapsed ? <span>{item.label}</span> : null}
                </>
              )}
            </NavLink>
          </RequirePermission>
        ))}
      </nav>

      <div className="border-t border-border p-2">
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="w-full justify-center"
          onClick={() => setCollapsed(!collapsed)}
          aria-label={collapsed ? '展开侧边栏' : '折叠侧边栏'}
        >
          {collapsed ? <CaretRight className="h-4 w-4" /> : <CaretLeft className="h-4 w-4" />}
        </Button>
      </div>
    </aside>
  );
}
