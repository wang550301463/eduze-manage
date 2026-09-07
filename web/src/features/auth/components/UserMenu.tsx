import { SignOut, User } from '@phosphor-icons/react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/DropdownMenu';
import { logout } from '@/features/auth/api';
import { useAuthStore } from '@/features/auth/store';
import { toast } from '@/lib/toast';

export function UserMenu(): JSX.Element {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const clearAuth = useAuthStore((s) => s.clearAuth);

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // 即使接口失败也本地登出
    } finally {
      clearAuth();
      toast.info('已退出登录');
      navigate('/login', { replace: true });
    }
  };

  const displayName = user?.name ?? user?.username ?? '用户';

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="sm" className="gap-2" aria-label="用户菜单">
          <User className="h-4 w-4" weight="duotone" />
          <span className="hidden max-w-[8rem] truncate sm:inline">{displayName}</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-48">
        <DropdownMenuLabel>{displayName}</DropdownMenuLabel>
        {user?.roles?.length ? (
          <DropdownMenuLabel className="font-normal text-muted-fg">
            {user.roles.join(' · ')}
          </DropdownMenuLabel>
        ) : null}
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={handleLogout} className="gap-2 text-destructive">
          <SignOut className="h-4 w-4" />
          退出登录
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
