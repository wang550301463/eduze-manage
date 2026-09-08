import { Link } from 'react-router-dom';
import { usePlatformList } from './hooks';
import { Notice } from './ui';
export function TeachingTasks({ branchId }: { branchId: string }) {
  const themes = usePlatformList<{ planned: number; inProgress: number; finished: number }>(
    `/teaching/dashboard?branchId=${branchId}`,
  );
  const portfolios = usePlatformList<{
    draft: number;
    published: number;
    makeupPending: number;
    needsPublishing: number;
  }>(`/portfolio/dashboard?branchId=${branchId}`);
  return (
    <section className="mt-5 rounded-3xl border border-white/80 bg-white/60 p-5">
      <div className="flex items-center justify-between">
        <h2 className="font-semibold">教学进展与课效待办</h2>
        <Link className="text-sm text-primary" to="/teaching/themes">
          管理班级主题
        </Link>
      </div>
      <Notice error={themes.error || portfolios.error} />
      <div className="mt-4 grid grid-cols-2 gap-3 md:grid-cols-4">
        {[
          { name: '进行中主题', value: themes.data?.inProgress, to: '/teaching/themes' },
          { name: '待整理课效', value: portfolios.data?.draft, to: '/portfolio' },
          { name: '更新待发布', value: portfolios.data?.needsPublishing, to: '/portfolio' },
          { name: '等待补课完成', value: portfolios.data?.makeupPending, to: '/portfolio' },
        ].map((item) => (
          <Link key={item.name} to={item.to} className="rounded-2xl bg-white/70 p-4">
            <span className="block text-xs text-muted-fg">{item.name}</span>
            <strong className="mt-2 block text-2xl">{item.value ?? '—'}</strong>
          </Link>
        ))}
      </div>
    </section>
  );
}
