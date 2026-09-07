import { useAuthStore } from '@/features/auth/store';

export function useDefaultBranchId(): number {
  const branchIds = useAuthStore((s) => s.branchIds);
  return branchIds[0] ?? 1;
}
