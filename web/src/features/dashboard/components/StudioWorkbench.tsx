import {
  ArrowRight,
  CalendarBlank,
  CheckSquare,
  Clock,
  Notebook,
  UsersThree,
} from '@phosphor-icons/react';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Button, buttonVariants } from '@/components/ui/Button';
import { SimpleSelect } from '@/components/ui/Select';
import { BranchAttendanceKpis } from '@/features/attendance/components/BranchAttendanceKpis';
import { fetchDashboardKpis } from '@/features/attendance/api';
import { useAuthStore } from '@/features/auth/store';
import { scheduleApi } from '@/features/teacher/api';
import { organizeLessons } from '../workbench-data';
import { StudioArtwork } from './StudioArtwork';
import { cn } from '@/lib/cn';

export function StudioWorkbench({ teaching = false }: { teaching?: boolean }): JSX.Element {
  const user = useAuthStore((s) => s.user);
  const permissions = useAuthStore((s) => s.permissions);
  const can = (permission: string) => permissions.includes(permission);
  const [selectedBranch, setSelectedBranch] = useState('');
  const branch =
    user?.branches.find((item) => String(item.id) === selectedBranch) ?? user?.branches[0];
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 60_000);
    return () => window.clearInterval(timer);
  }, []);
  const schedule = useQuery({
    queryKey: ['studio-schedule', user?.id, teaching, branch?.id, now.toDateString()],
    queryFn: () => (teaching ? scheduleApi.myWeek() : scheduleApi.byTeacher(String(branch!.id))),
    enabled: can('lesson:read') && (teaching || !!branch),
    refetchInterval: 60_000,
  });
  const kpis = useQuery({
    queryKey: ['dashboard-kpis', branch?.id],
    queryFn: () => fetchDashboardKpis(branch!.id),
    enabled: !teaching && can('stat:read') && !!branch,
  });
  const organized = organizeLessons(schedule.data, now);
  const nextLesson = organized.upcoming[0];
  const dateLabel = new Intl.DateTimeFormat('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  }).format(now);
  const hour = now.getHours();
  const greeting = hour < 12 ? '上午好' : hour < 18 ? '下午好' : '晚上好';
  const shortcuts = [
    {
      title: '课堂签到',
      detail: '查看到课，记录接送',
      path: '/attendance',
      permission: 'attendance:read',
      icon: CheckSquare,
      tone: 'bg-[#eaf1ed] text-[#4f7965]',
    },
    {
      title: '请假审批',
      detail: '处理申请，安排补课',
      path: '/attendance/leaves',
      permission: 'leave:read',
      icon: Notebook,
      tone: 'bg-[#f7efe5] text-[#9a764e]',
    },
    {
      title: '学员档案',
      detail: '家长联系与学习记录',
      path: '/students',
      permission: 'student:read',
      icon: UsersThree,
      tone: 'bg-[#eeeef6] text-[#77729a]',
    },
    {
      title: '教学时段',
      detail: '维护老师可用时间',
      path: '/teachers/availabilities',
      permission: 'teacher:availability:read',
      icon: Clock,
      tone: 'bg-[#eaf0f4] text-[#577d91]',
    },
  ].filter((item) => can(item.permission));
  return (
    <div
      className="mx-auto max-w-[1400px] space-y-6 pb-6"
      data-testid={teaching ? 'my-workbench' : 'principal-workbench'}
    >
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="studio-eyebrow mb-2">
            {teaching ? 'MY TEACHING' : 'STUDIO OVERVIEW'}{' '}
            <span className="ml-2 font-normal tracking-normal">/ {dateLabel}</span>
          </p>
          <h1 className="text-[26px] font-semibold tracking-tight">
            {greeting}，{user?.name || '欢迎回来'}
          </h1>
        </div>
        {!teaching && (
          <div className="flex items-center gap-2 text-xs text-muted-fg">
            <span>当前校区</span>
            {branch ? (
              <SimpleSelect
                aria-label="工作台校区"
                value={String(branch.id)}
                onValueChange={setSelectedBranch}
                options={(user?.branches ?? []).map((item) => ({
                  value: String(item.id),
                  label: item.name,
                }))}
                className="w-40"
              />
            ) : (
              <span>尚未分配校区</span>
            )}
          </div>
        )}
      </div>
      <section className="studio-hero flex min-h-[206px] items-center justify-between px-6 py-7 sm:px-8">
        <div className="relative z-10 max-w-[500px]">
          <p className="mb-3 text-xs font-medium text-primary">
            {teaching ? '专注每一堂课的美好发生' : '让教学有序，让创作自由'}
          </p>
          <h2 className="text-[27px] font-medium leading-snug tracking-tight sm:text-[32px]">
            把今天，留给创作。
          </h2>
          <p className="mt-3 text-[13px] leading-6 text-muted-fg">
            {teaching
              ? '从课前准备到课堂记录，把时间留给孩子。'
              : '课表、学员与日常事务，在一个空间里从容安排。'}
          </p>
          {can('lesson:read') && (
            <Link to="/schedule" className={cn(buttonVariants({ size: 'sm' }), 'mt-5 gap-2')}>
              查看本周课表
              <ArrowRight className="h-4 w-4" />
            </Link>
          )}
        </div>
        <div className="-my-7 -mr-4 hidden shrink-0 md:block">
          <StudioArtwork />
        </div>
      </section>
      {!teaching && can('stat:read') && !!branch && (
        <section aria-label="校区经营概览">
          {kpis.isError ? (
            <div className="studio-panel flex items-center justify-between p-5 text-sm">
              <span>运营数据暂时无法加载</span>
              <Button size="sm" variant="secondary" onClick={() => void kpis.refetch()}>
                重试
              </Button>
            </div>
          ) : kpis.isPending ? (
            <div className="studio-panel p-6 text-sm text-muted-fg" role="status">
              正在读取校区数据…
            </div>
          ) : (
            <BranchAttendanceKpis data={kpis.data} />
          )}
        </section>
      )}
      <div className="grid items-start gap-5 xl:grid-cols-[minmax(0,1.65fr)_minmax(280px,1fr)]">
        <section className="studio-panel min-w-0 overflow-hidden">
          <div className="flex items-center justify-between border-b border-border/50 px-6 py-5">
            <div className="flex items-center gap-3">
              <CalendarBlank className="h-5 w-5 text-primary" weight="duotone" />
              <h2 className="text-[15px] font-semibold">
                {teaching ? '我的今日课堂' : '今日课堂'}
              </h2>
              {schedule.data && (
                <span className="rounded-md bg-muted/70 px-2 py-0.5 text-[11px] text-muted-fg">
                  {organized.today.length} 节
                </span>
              )}
            </div>
            {can('lesson:read') && (
              <Link
                to="/schedule"
                className="flex items-center gap-1 text-xs text-muted-fg hover:text-primary"
              >
                全部课表
                <ArrowRight />
              </Link>
            )}
          </div>
          {!can('lesson:read') ? (
            <p className="px-6 py-9 text-sm text-muted-fg">当前账号未开通课表权限。</p>
          ) : schedule.isError ? (
            <div className="space-y-3 p-6">
              <p className="text-sm text-muted-fg">课表加载失败，请重试。</p>
              <Button variant="secondary" size="sm" onClick={() => void schedule.refetch()}>
                重新加载
              </Button>
            </div>
          ) : schedule.isPending && (teaching || !!branch) ? (
            <p role="status" className="p-6 text-sm text-muted-fg">
              正在整理课表…
            </p>
          ) : organized.today.length ? (
            <div className="divide-y divide-border/40 px-6">
              {organized.today.map((lesson) => {
                const finished =
                  new Date(lesson.endAt) <= now || lesson.status.toLowerCase() === 'completed';
                const active = !finished && new Date(lesson.startAt) <= now;
                return (
                  <Link
                    key={lesson.lessonId}
                    to={`/schedule?lessonId=${encodeURIComponent(lesson.lessonId)}`}
                    className="group -mx-2 flex items-center gap-4 rounded-lg px-2 py-4 transition-colors hover:bg-muted/50"
                  >
                    <div className="w-12 shrink-0 tabular-nums">
                      <p className="text-[15px] font-medium">{lesson.startAt.slice(11, 16)}</p>
                      <p className="mt-1 text-[11px] text-muted-fg">{lesson.endAt.slice(11, 16)}</p>
                    </div>
                    <div
                      className={cn(
                        'h-9 w-[3px] shrink-0 rounded-full',
                        finished ? 'bg-border' : 'bg-[#93ada0]',
                      )}
                    />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-[13px] font-medium">
                        {lesson.teacherName} · {lesson.classRoomName || '教室待安排'}
                      </p>
                      <p className="mt-1 text-[11px] text-muted-fg">
                        {lesson.studentCount} 位学员
                        {lesson.capacity ? ` · 容量 ${lesson.capacity} 人` : ''}
                      </p>
                    </div>
                    <span
                      className={cn(
                        'shrink-0 text-[11px]',
                        active ? 'text-success' : 'text-muted-fg',
                      )}
                    >
                      {finished ? '已结束' : active ? '进行中' : '待上课'}
                    </span>
                    <ArrowRight className="h-4 w-4 shrink-0 text-muted-fg group-hover:text-primary" />
                  </Link>
                );
              })}
            </div>
          ) : (
            <div className="px-6 py-10 text-center">
              <CalendarBlank className="mx-auto mb-3 h-8 w-8 text-muted-fg/45" weight="duotone" />
              <p className="text-sm font-medium">
                {branch || teaching ? '今天没有安排课次' : '分配校区后，即可查看课堂'}
              </p>
              <p className="mt-2 text-xs text-muted-fg">
                {nextLesson
                  ? `本周下一课：${nextLesson.startAt.slice(5, 10).replace('-', '月')}日 ${nextLesson.startAt.slice(11, 16)}`
                  : '可以查看周课表，提前安排接下来的教学。'}
              </p>
              {nextLesson && (
                <Link
                  className="mt-4 inline-block text-xs font-medium text-primary"
                  to={`/schedule?lessonId=${encodeURIComponent(nextLesson.lessonId)}`}
                >
                  查看下一课 →
                </Link>
              )}
            </div>
          )}
          {schedule.data && (
            <div className="border-t border-border/50 bg-white/40 px-6 py-3 text-[11px] text-muted-fg">
              {teaching ? '我的课表' : branch?.name} · 本周共 {organized.total} 节有效课次
            </div>
          )}
        </section>
        <section className="studio-panel p-3">
          <div className="px-3 pb-3 pt-2">
            <p className="studio-eyebrow mb-2">DAILY ESSENTIALS</p>
            <h2 className="text-[15px] font-semibold">日常事务</h2>
          </div>
          {shortcuts.map((item) => (
            <Link key={item.path} to={item.path} className="studio-action">
              <span
                className={cn(
                  'flex h-10 w-10 shrink-0 items-center justify-center rounded-[13px]',
                  item.tone,
                )}
              >
                <item.icon className="h-5 w-5" weight="duotone" />
              </span>
              <div className="flex-1">
                <p className="text-[13px] font-medium">{item.title}</p>
                <p className="mt-1 text-[11px] text-muted-fg">{item.detail}</p>
              </div>
              <ArrowRight className="h-4 w-4 text-muted-fg" />
            </Link>
          ))}
          {!shortcuts.length && (
            <p className="p-3 text-sm text-muted-fg">联系管理员开通教学功能。</p>
          )}
          <p className="mx-3 mb-1 mt-3 border-t border-border/50 pt-4 text-[11px] leading-5 text-muted-fg">
            按 ⌘ K，快速找到学员、家长或课次。
          </p>
        </section>
      </div>
    </div>
  );
}
