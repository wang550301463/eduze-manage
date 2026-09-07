import { CaretLeft, CaretRight } from '@phosphor-icons/react';
import { NavLink } from 'react-router-dom';
import { RequirePermission } from '@/components/auth/RequirePermission';
import { Button } from '@/components/ui/Button';
import { mainNavItems, navItemEnd, settingsNavItems } from '@/app/shell/nav-config';
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
        'hidden h-full shrink-0 flex-col border-r border-border/80 bg-[#F2F2F7] lg:flex',
        collapsed ? 'w-16' : 'w-[210px]',
        className,
      )}
      aria-label="主导航"
    >
      <nav className="flex flex-1 flex-col gap-1 overflow-y-auto p-2">
        {mainNavItems.map((item) => (
          <RequirePermission key={item.path} perm={item.permission}>
            <NavLink
              to={item.path}
              end={navItemEnd(item.path)}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-[10px] px-3 py-2 text-sm transition-colors',
                  isActive
                    ? 'bg-primary font-medium text-primary-fg'
                    : 'text-foreground/80 hover:bg-muted',
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
              end={navItemEnd(item.path)}
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
