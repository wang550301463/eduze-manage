import { useAuthStore } from '@/features/auth/store';
import { Outlet } from 'react-router-dom';
import { CommandPalette } from '@/app/shell/CommandPalette';
import { Header } from '@/app/shell/Header';
import { MobileNav } from '@/app/shell/MobileNav';
import { ShortcutHelpDialog } from '@/app/shell/ShortcutHelpDialog';
import { Sidebar } from '@/app/shell/Sidebar';
import { useShell } from '@/app/shell/shell-context';

export function AppLayout(): JSX.Element {
  const user = useAuthStore((s) => s.user);
  const scopeKey = JSON.stringify([user?.id, user?.roles, user?.branches, user?.permissions]);
  const { setPaletteOpen, setMobileNavOpen, helpOpen, setHelpOpen } = useShell();

  return (
    <div className="studio-background studio-shell">
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-primary focus:px-3 focus:py-2 focus:text-primary-fg"
      >
        跳过导航
      </a>
      <Sidebar />
      <MobileNav />
      <div className="studio-window">
        <Header
          onOpenSearch={() => setPaletteOpen(true)}
          onOpenMobileNav={() => setMobileNavOpen(true)}
        />
        <main id="main" className="min-h-0 flex-1 overflow-y-auto p-4 md:p-7 lg:px-8">
          <Outlet key={scopeKey} />
        </main>
      </div>
      <CommandPalette />
      <ShortcutHelpDialog open={helpOpen} onOpenChange={setHelpOpen} />
    </div>
  );
}
