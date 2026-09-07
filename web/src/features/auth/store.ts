import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { UserInfo } from './types';

export type AuthState = {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserInfo | null;
  branchIds: number[];
  permissions: string[];
  sidebarCollapsed: boolean;
  setAuth: (payload: {
    accessToken: string;
    refreshToken: string;
    user: UserInfo;
  }) => void;
  setAccessToken: (accessToken: string) => void;
  setUser: (user: UserInfo) => void;
  setSidebarCollapsed: (collapsed: boolean) => void;
  clearAuth: () => void;
};

function deriveFromUser(user: UserInfo): Pick<AuthState, 'branchIds' | 'permissions'> {
  return {
    branchIds: user.branches.map((b) => b.id),
    permissions: user.permissions ?? [],
  };
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      branchIds: [],
      permissions: [],
      sidebarCollapsed: false,
      setAuth: ({ accessToken, refreshToken, user }) =>
        set({
          accessToken,
          refreshToken,
          user,
          ...deriveFromUser(user),
        }),
      setAccessToken: (accessToken) => set({ accessToken }),
      setUser: (user) =>
        set({
          user,
          ...deriveFromUser(user),
        }),
      setSidebarCollapsed: (sidebarCollapsed) => set({ sidebarCollapsed }),
      clearAuth: () =>
        set({
          accessToken: null,
          refreshToken: null,
          user: null,
          branchIds: [],
          permissions: [],
        }),
    }),
    {
      name: 'eduze-auth',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
        branchIds: state.branchIds,
        permissions: state.permissions,
        sidebarCollapsed: state.sidebarCollapsed,
      }),
    },
  ),
);

export function isAuthenticated(): boolean {
  return Boolean(useAuthStore.getState().accessToken);
}
