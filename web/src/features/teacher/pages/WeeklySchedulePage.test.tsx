import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { scheduleApi } from '@/features/teacher/api';
import { fetchLesson } from '@/features/lesson/api';
import { WeeklySchedulePage } from './WeeklySchedulePage';
import { LessonRosterTable } from '@/features/lesson/components/LessonRosterTable';
import { useAuthStore } from '@/features/auth/store';

vi.mock('@/features/student/api', () => ({
  branchApi: { list: vi.fn().mockResolvedValue([{ id: '22', name: '南校区', code: 'S' }]) },
}));
vi.mock('@/features/teacher/api', () => ({
  scheduleApi: {
    byTeacher: vi.fn(),
    lessonStudents: vi.fn(),
    bulkGenerate: vi.fn(),
    createLesson: vi.fn(),
  },
  teacherApi: { list: vi.fn() },
}));
vi.mock('@/features/lesson/api', () => ({
  fetchLesson: vi.fn(),
  fetchLessonLogs: vi.fn().mockResolvedValue([]),
  checkConflict: vi.fn(),
  rescheduleLesson: vi.fn(),
  cancelLesson: vi.fn(),
}));
vi.mock('@/features/attendance/api', () => ({
  fetchTodayRoster: vi.fn().mockResolvedValue({ items: [] }),
}));

const lessonId = '9007199254740993123';
function RouteState() {
  return <div data-testid="route">{useLocation().search}</div>;
}
function setup(entry = '/schedule') {
  return render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={[entry]}
      >
        <WeeklySchedulePage />
        <RouteState />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('teacher weekly schedule selection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ permissions: ['lesson:read'] });
    vi.mocked(scheduleApi.byTeacher).mockResolvedValue({
      weekStart: '2026-09-07',
      columns: [
        {
          teacherId: '11',
          teacherName: '王老师',
          branchId: '22',
          lessons: [
            {
              lessonId,
              startAt: '2026-09-09T14:00:00',
              endAt: '2026-09-09T15:30:00',
              dayOfWeek: 3,
              startMinute: 840,
              endMinute: 930,
              studentCount: 1,
              classRoomName: '一号画室',
              status: 'SCHEDULED',
            },
          ],
        },
      ],
    });
    vi.mocked(fetchLesson).mockResolvedValue({
      id: lessonId,
      branchId: '22',
      classGroupId: '9',
      classGroupName: '水彩一班',
      teacherName: '王老师',
      startAt: '2026-09-09T14:00:00',
      endAt: '2026-09-09T15:30:00',
      status: 'SCHEDULED',
    });
    vi.mocked(scheduleApi.lessonStudents).mockResolvedValue([
      {
        id: '31',
        lessonId,
        studentId: '51',
        studentName: '本节课学员',
        source: 'MANUAL',
        status: 'BOOKED',
      },
    ]);
  });

  it('opens the chosen lesson by keyboard and reads only its roster', async () => {
    setup();
    const card = await screen.findByRole('button', { name: /王老师.*14:00.*15:30/ });
    card.focus();
    await userEvent.keyboard('{Enter}');
    expect(await screen.findByRole('dialog', { name: '水彩一班' })).toBeInTheDocument();
    expect(await screen.findByText('本节课学员')).toBeInTheDocument();
    expect(scheduleApi.lessonStudents).toHaveBeenCalledWith(lessonId);
    expect(screen.getByTestId('route')).toHaveTextContent(`openId=${lessonId}`);
    fireEvent.click(screen.getByRole('button', { name: '关闭' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.getByTestId('route')).not.toHaveTextContent('openId');
  });

  it.each(['openId', 'lessonId'])(
    'opens a %s deep link and locates the actual week and branch',
    async (key) => {
      setup(`/schedule?${key}=${lessonId}`);
      expect(await screen.findByRole('dialog', { name: '水彩一班' })).toBeInTheDocument();
      await waitFor(() => expect(scheduleApi.byTeacher).toHaveBeenCalledWith('22', '2026-09-07'));
      expect(fetchLesson).toHaveBeenCalledWith(lessonId);
    },
  );

  it('shows a loading state rather than an empty week while the schedule is pending', () => {
    vi.mocked(scheduleApi.byTeacher).mockReturnValue(new Promise(() => undefined));
    setup();
    expect(screen.getByRole('status')).toHaveTextContent('正在加载课表');
    expect(screen.queryByText(/本周暂无课次/)).not.toBeInTheDocument();
  });

  it('hides lesson creation controls and their empty-state instructions without write permission', async () => {
    vi.mocked(scheduleApi.byTeacher).mockResolvedValue({ weekStart: '2026-09-07', columns: [] });
    setup();
    expect(await screen.findByText('本周暂无课次')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '新建特殊课' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '批量排课' })).not.toBeInTheDocument();
    expect(screen.queryByText(/点击右上「批量排课」/)).not.toBeInTheDocument();
  });

  it('keeps lesson creation available to users with write permission', async () => {
    useAuthStore.setState({ permissions: ['lesson:read', 'lesson:write'] });
    setup();
    expect(screen.getByRole('button', { name: '新建特殊课' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '批量排课' }));
    expect(await screen.findByRole('dialog', { name: '批量排课' })).toBeInTheDocument();
  });

  it('keeps rosters for simultaneous lessons separate in the query cache', async () => {
    const otherLessonId = '9007199254740993124';
    vi.mocked(scheduleApi.lessonStudents).mockImplementation(async (id) => [
      {
        id: `row-${id}`,
        lessonId: id,
        studentId: `student-${id}`,
        studentName: id === lessonId ? '水彩学员' : '素描学员',
        source: 'MANUAL',
        status: 'BOOKED',
      },
    ]);
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const view = (id: string) => (
      <QueryClientProvider client={client}>
        <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
          <LessonRosterTable lessonId={id} />
        </MemoryRouter>
      </QueryClientProvider>
    );
    const { rerender } = render(view(lessonId));
    expect(await screen.findByText('水彩学员')).toBeInTheDocument();
    rerender(view(otherLessonId));
    expect(await screen.findByText('素描学员')).toBeInTheDocument();
    expect(screen.queryByText('水彩学员')).not.toBeInTheDocument();
    expect(scheduleApi.lessonStudents).toHaveBeenCalledWith(otherLessonId);
    expect(screen.getByRole('link', { name: '素描学员' })).toHaveAttribute(
      'href',
      `/students?openId=student-${otherLessonId}`,
    );
  });
});
