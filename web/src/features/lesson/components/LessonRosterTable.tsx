import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { scheduleApi } from '@/features/teacher/api';

const STATUS_LABELS: Record<string, string> = {
  BOOKED: '已预约',
  CHECKED_IN: '已签到',
  CHECKED_OUT: '已签退',
  PRESENT: '已出勤',
  ABSENT: '缺勤',
  LEAVE: '请假',
  CANCELLED: '已移出',
};

export function LessonRosterTable({ lessonId }: { lessonId: string | number }): JSX.Element {
  const { data, isPending, isError, refetch } = useQuery({
    queryKey: ['lesson-students', String(lessonId)],
    queryFn: () => scheduleApi.lessonStudents(String(lessonId)),
  });

  if (isPending) return <Skeleton className="h-32 w-full" />;
  if (isError)
    return (
      <div role="alert" className="space-y-2 text-sm">
        <p>学员名单加载失败</p>
        <Button size="sm" variant="secondary" onClick={() => void refetch()}>
          重试名单
        </Button>
      </div>
    );
  const items = data ?? [];
  if (!items.length)
    return <p className="rounded-xl bg-muted/50 p-5 text-center text-muted-fg">本节课暂无学员</p>;

  return (
    <div className="space-y-3">
      <p className="text-xs text-muted-fg">
        本节课 {items.filter((item) => item.status !== 'CANCELLED').length} 名学员
      </p>
      <ul className="divide-y divide-border overflow-hidden rounded-xl border border-border">
        {items.map((item) => (
          <li key={item.id} className="flex items-center justify-between gap-3 px-4 py-3">
            <Link
              to={`/students?openId=${encodeURIComponent(item.studentId)}`}
              className="font-medium text-primary hover:underline"
            >
              {item.studentName ?? '查看学员'}
            </Link>
            <span className="text-xs text-muted-fg">
              {STATUS_LABELS[item.status] ?? item.status}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
