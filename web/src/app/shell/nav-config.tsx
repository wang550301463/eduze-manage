import {
  Buildings,
  Calendar,
  ChalkboardTeacher,
  ClipboardText,
  GraduationCap,
  UsersThree,
  type IconProps,
} from '@phosphor-icons/react';
import type { ComponentType } from 'react';

export type NavItem = {
  label: string;
  path: string;
  icon: ComponentType<IconProps>;
  permission?: string;
};

export const mainNavItems: NavItem[] = [
  { label: '工作台', path: '/', icon: GraduationCap },
  { label: '我的工作台', path: '/workbench', icon: GraduationCap, permission: 'lesson:teacher_view' },
  { label: '学员', path: '/students', icon: UsersThree, permission: 'student:read' },
  { label: '课程记录', path: '/lesson-history', icon: ClipboardText, permission: 'student:read' },
  { label: '老师可用时段', path: '/teachers/availabilities', icon: ChalkboardTeacher, permission: 'teacher:availability:read' },
  { label: '课程', path: '/courses', icon: ChalkboardTeacher, permission: 'course:read' },
  { label: '分组标签', path: '/courses/class-groups', icon: UsersThree, permission: 'classgroup:read' },
  { label: '周课表', path: '/schedule', icon: Calendar, permission: 'lesson:read' },
  { label: '签到', path: '/attendance', icon: ClipboardText, permission: 'attendance:read' },
  { label: '请假', path: '/attendance/leaves', icon: ClipboardText, permission: 'leave:read' },
];

export const settingsNavItems: NavItem[] = [
  { label: '校区', path: '/settings/branches', icon: Buildings, permission: 'branch:read' },
  { label: '账号', path: '/settings/users', icon: UsersThree, permission: 'user:read' },
  { label: '角色', path: '/settings/roles', icon: UsersThree, permission: 'role:read' },
];

export const allNavItems: NavItem[] = [...mainNavItems, ...settingsNavItems];

/**
 * 是否对 NavLink 启用 end 匹配。
 * 当存在更长的同前缀导航项时（如 /attendance vs /attendance/leaves），
 * 必须 end，否则父级与子级会同时高亮。
 */
export function navItemEnd(path: string, items: NavItem[] = allNavItems): boolean {
  if (path === '/') return true;
  return items.some((other) => other.path !== path && other.path.startsWith(`${path}/`));
}

/** 按当前路径解析面包屑标题（最长前缀匹配） */
export function getNavLabel(pathname: string): string {
  const sorted = [...allNavItems].sort((a, b) => b.path.length - a.path.length);
  for (const item of sorted) {
    if (item.path === '/') {
      if (pathname === '/') return item.label;
      continue;
    }
    if (pathname === item.path || pathname.startsWith(`${item.path}/`)) {
      return item.label;
    }
  }
  return '工作台';
}
