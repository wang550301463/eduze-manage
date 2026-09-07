import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';

export function useUrlState<T extends Record<string, string | undefined>>(
  defaults: T,
): [T, (patch: Partial<T>) => void] {
  const [params, setParams] = useSearchParams();

  const state = useMemo(() => {
    const next = { ...defaults };
    for (const key of Object.keys(defaults) as (keyof T)[]) {
      const v = params.get(String(key));
      if (v !== null && v !== '') {
        next[key] = v as T[keyof T];
      }
    }
    return next;
  }, [params, defaults]);

  const setState = useCallback(
    (patch: Partial<T>) => {
      const next = new URLSearchParams(params);
      for (const [key, value] of Object.entries(patch)) {
        if (value === undefined || value === '') {
          next.delete(key);
        } else {
          next.set(key, value);
        }
      }
      setParams(next, { replace: true });
    },
    [params, setParams],
  );

  return [state, setState];
}
