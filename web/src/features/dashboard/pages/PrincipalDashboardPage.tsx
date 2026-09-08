import { useAuthStore } from '@/features/auth/store';
import { StudioWorkbench } from '../components/StudioWorkbench';
import { prefersTeachingHome } from '../home-role';

export function PrincipalDashboardPage(): JSX.Element {
  const user = useAuthStore((state) => state.user);
  const roles = user?.roles ?? [];
  return <StudioWorkbench teaching={prefersTeachingHome(roles)} />;
}
