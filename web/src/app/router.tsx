import { createBrowserRouter, Navigate } from 'react-router-dom';
import { lazy } from 'react';
import { AppLayout } from '@/app/shell/AppLayout';
import { ProtectedRoute } from '@/app/router/ProtectedRoute';
import { LoginPage } from '@/features/auth/pages/LoginPage';
import { LazyPage } from '@/app/LazyPage';

const PrincipalDashboardPage = lazy(() =>
  import('@/features/dashboard/pages/PrincipalDashboardPage').then((m) => ({
    default: m.PrincipalDashboardPage,
  })),
);
const AttendanceWorkbenchPage = lazy(() =>
  import('@/features/attendance/pages/AttendanceWorkbenchPage').then((m) => ({
    default: m.AttendanceWorkbenchPage,
  })),
);
const LeaveListPage = lazy(() =>
  import('@/features/attendance/pages/LeaveListPage').then((m) => ({ default: m.LeaveListPage })),
);
const AttendanceStatsPage = lazy(() =>
  import('@/features/attendance/pages/AttendanceStatsPage').then((m) => ({
    default: m.AttendanceStatsPage,
  })),
);
const StudentListPage = lazy(() =>
  import('@/features/student/pages/StudentListPage').then((m) => ({ default: m.StudentListPage })),
);
const StudentLessonHistoryPage = lazy(() =>
  import('@/features/student/pages/StudentLessonHistoryPage').then((m) => ({
    default: m.StudentLessonHistoryPage,
  })),
);
const CourseListPage = lazy(() =>
  import('@/features/course/pages/CourseListPage').then((m) => ({ default: m.CourseListPage })),
);
const ClassGroupListPage = lazy(() =>
  import('@/features/course/pages/ClassGroupListPage').then((m) => ({
    default: m.ClassGroupListPage,
  })),
);
const ClassRoomListPage = lazy(() =>
  import('@/features/course/pages/ClassRoomListPage').then((m) => ({
    default: m.ClassRoomListPage,
  })),
);
const WeeklySchedulePage = lazy(() =>
  import('@/features/teacher/pages/WeeklySchedulePage').then((m) => ({
    default: m.WeeklySchedulePage,
  })),
);
const SchedulePage = lazy(() =>
  import('@/features/schedule/pages/SchedulePage').then((m) => ({ default: m.SchedulePage })),
);
const TeacherAvailabilityPage = lazy(() =>
  import('@/features/teacher/pages/TeacherAvailabilityPage').then((m) => ({
    default: m.TeacherAvailabilityPage,
  })),
);
const MyWorkbenchPage = lazy(() =>
  import('@/features/teacher/pages/MyWorkbenchPage').then((m) => ({ default: m.MyWorkbenchPage })),
);
const BranchListPage = lazy(() =>
  import('@/features/settings/pages/BranchListPage').then((m) => ({ default: m.BranchListPage })),
);
const UserListPage = lazy(() =>
  import('@/features/settings/pages/UserListPage').then((m) => ({ default: m.UserListPage })),
);
const RoleListPage = lazy(() =>
  import('@/features/settings/pages/RoleListPage').then((m) => ({ default: m.RoleListPage })),
);
const AttendancePage = lazy(() =>
  import('@/pages/AttendancePage').then((m) => ({ default: m.AttendancePage })),
);

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/',
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          {
            index: true,
            element: (
              <LazyPage>
                <PrincipalDashboardPage />
              </LazyPage>
            ),
          },
          {
            path: 'attendance',
            element: (
              <ProtectedRoute permissions={['attendance:read']}>
                <LazyPage>
                  <AttendanceWorkbenchPage />
                </LazyPage>
              </ProtectedRoute>
            ),
          },
          {
            path: 'attendance/leaves',
            element: (
              <ProtectedRoute permissions={['leave:read']}>
                <LazyPage>
                  <LeaveListPage />
                </LazyPage>
              </ProtectedRoute>
            ),
          },
          {
            path: 'attendance/stats',
            element: (
              <ProtectedRoute permissions={['stat:read']}>
                <LazyPage>
                  <AttendanceStatsPage />
                </LazyPage>
              </ProtectedRoute>
            ),
          },
          {
            path: 'students',
            element: (
              <ProtectedRoute permissions={['student:read']}>
                <LazyPage>
                  <StudentListPage />
                </LazyPage>
              </ProtectedRoute>
            ),
          },
          {
            path: 'lesson-history',
            element: (
              <ProtectedRoute permissions={['student:read']}>
                <LazyPage>
                  <StudentLessonHistoryPage />
                </LazyPage>
              </ProtectedRoute>
            ),
          },
          {
            path: 'courses',
            element: (
              <ProtectedRoute permissions={['course:read']}>
                <LazyPage>
                  <CourseListPage />
                </LazyPage>
              </ProtectedRoute>
            ),
          },
          {
            path: 'courses/class-groups',
            element: (
              <ProtectedRoute permissions={['classgroup:read']}>
                <LazyPage>
                  <ClassGroupListPage />
                </LazyPage>
              </ProtectedRoute>
            ),
          },
          {
            path: 'courses/class-rooms',
            element: (
              <ProtectedRoute permissions={['classroom:read']}>
                <LazyPage>
                  <ClassRoomListPage />
                </LazyPage>
              </ProtectedRoute>
            ),
          },
          {
            path: 'schedule',
            element: (
              <ProtectedRoute permissions={['lesson:read']}>
                <LazyPage>
                  <WeeklySchedulePage />
                </LazyPage>
              </ProtectedRoute>
            ),
          },
          {
            path: 'schedule/legacy',
            element: (
              <ProtectedRoute permissions={['lesson:read']}>
                <LazyPage>
                  <SchedulePage />
                </LazyPage>
              </ProtectedRoute>
            ),
          },
          {
            path: 'teachers/availabilities',
            element: (
              <ProtectedRoute permissions={['teacher:availability:read']}>
                <LazyPage>
                  <TeacherAvailabilityPage />
                </LazyPage>
              </ProtectedRoute>
            ),
          },
          {
            path: 'workbench',
            element: (
              <ProtectedRoute permissions={['lesson:read']}>
                <LazyPage>
                  <MyWorkbenchPage />
                </LazyPage>
              </ProtectedRoute>
            ),
          },
          {
            path: 'settings/branches',
            element: (
              <ProtectedRoute permissions={['branch:read']}>
                <LazyPage>
                  <BranchListPage />
                </LazyPage>
              </ProtectedRoute>
            ),
          },
          {
            path: 'settings/users',
            element: (
              <ProtectedRoute permissions={['user:read']}>
                <LazyPage>
                  <UserListPage />
                </LazyPage>
              </ProtectedRoute>
            ),
          },
          {
            path: 'settings/roles',
            element: (
              <ProtectedRoute permissions={['role:read']}>
                <LazyPage>
                  <RoleListPage />
                </LazyPage>
              </ProtectedRoute>
            ),
          },
          {
            path: 'attendance-legacy',
            element: (
              <ProtectedRoute permissions={['attendance:read']}>
                <LazyPage>
                  <AttendancePage />
                </LazyPage>
              </ProtectedRoute>
            ),
          },
          { path: '*', element: <Navigate to="/" replace /> },
        ],
      },
    ],
  },
]);
