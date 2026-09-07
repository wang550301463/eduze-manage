import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useAuthStore } from '@/features/auth/store';
import { scheduleApi } from '../api';

function fmtTime(min: number): string {
  const h = Math.floor(min / 60);
  const m = min % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

const DOW_LABELS = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'];

export function MyWorkbenchPage(): JSX.Element {
  const user = useAuthStore((s) => s.user);
  const { data, isLoading } = useQuery({
    queryKey: ['my-week'],
    queryFn: () => scheduleApi.myWeek(),
  });

  const col = data?.columns[0];
  const lessons = col?.lessons ?? [];

  return (
    <div className="space-y-4 pb-24" data-testid="my-workbench">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">
          {user?.name}，下午好
        </h1>
        <span className="text-sm text-muted-fg">老师工作台</span>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 md:grid-cols-4">
        <div className="rounded-xl border border-border bg-white p-4">
          <div className="text-2xl font-semibold">{lessons.length}</div>
          <div className="text-sm text-muted-fg">📅 本周课次</div>
        </div>
        <div className="rounded-xl border border-border bg-white p-4">
          <div className="text-2xl font-semibold">-</div>
          <div className="text-sm text-muted-fg">👶 主带学员</div>
        </div>
        <div className="rounded-xl border border-border bg-white p-4">
          <div className="text-2xl font-semibold">-</div>
          <div className="text-sm text-muted-fg">📝 待批请假</div>
        </div>
        <Link
          to="/teachers/availabilities"
          className="rounded-xl border border-border bg-white p-4 hover:bg-muted"
        >
          <div className="text-lg font-semibold">→</div>
          <div className="text-sm text-muted-fg">🕘 维护可用时段</div>
        </Link>
      </div>

      <div className="rounded-xl border border-border bg-white p-4">
        <h2 className="mb-3 text-sm font-semibold">本周课表</h2>
        {isLoading ? (
          <p className="text-sm text-muted-fg">加载中...</p>
        ) : lessons.length === 0 ? (
          <p className="text-sm text-muted-fg">本周暂无课次</p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {lessons.map((l) => (
              <Link
                key={String(l.lessonId)}
                to={`/schedule?lessonId=${l.lessonId}`}
                className="rounded-md border border-border px-3 py-2 text-sm hover:bg-muted"
              >
                <div className="font-medium">
                  {DOW_LABELS[l.dayOfWeek]} {fmtTime(l.startMinute)}-{fmtTime(l.endMinute)}
                </div>
                <div className="text-xs text-muted-fg">
                  {l.classRoomName ?? '-'} · {l.studentCount}{l.capacity ? `/${l.capacity}` : ''} 人
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
