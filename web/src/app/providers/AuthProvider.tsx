import { useEffect, type ReactNode } from 'react';
import { getMe } from '@/features/auth/api';
import { useAuthStore } from '@/features/auth/store';

type AuthProviderProps = {
  children: ReactNode;
};

/** 启动时用 token 拉取当前用户（若已持久化登录态） */
export function AuthProvider({ children }: AuthProviderProps): JSX.Element {
  const accessToken = useAuthStore((s) => s.accessToken);
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const clearAuth = useAuthStore((s) => s.clearAuth);

  useEffect(() => {
    if (!accessToken || user) return;
    void getMe()
      .then(setUser)
      .catch(() => clearAuth());
  }, [accessToken, user, setUser, clearAuth]);

  return <>{children}</>;
}
