import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/features/auth/store';
import { platformRequest } from './client';
export function usePlatformList<T>(path: string, enabled = true) {
  const user = useAuthStore((s) => s.user);
  return useQuery({
    queryKey: ['platform', user?.id, user?.roles, user?.branches, user?.permissions, path],
    queryFn: () => platformRequest<T>(path),
    enabled,
    retry: false,
  });
}
export function useAction() {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [success, setSuccess] = useState('');
  const cache = useQueryClient();
  const run = async (action: () => Promise<unknown>, message = '已保存') => {
    setBusy(true);
    setError(null);
    setSuccess('');
    try {
      await action();
      await cache.invalidateQueries({ queryKey: ['platform'] });
      setSuccess(message);
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  };
  return { busy, error, success, run };
}
