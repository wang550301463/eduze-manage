import { useAuthStore } from '@/features/auth/store';

export function hasPermission(perm: string): boolean {
  const { permissions } = useAuthStore.getState();
  return permissions.includes(perm);
}

export function hasAnyPermission(perms: string[]): boolean {
  if (perms.length === 0) return true;
  return perms.some((p) => hasPermission(p));
}

export function useHasPermission(perm: string): boolean {
  return useAuthStore((s) => s.permissions.includes(perm));
}
