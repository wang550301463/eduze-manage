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
  { label: '招生与经营', path: '/engagement', icon: Buildings, permission: 'engagement:write' },
  { label: '商城与订单', path: '/commerce', icon: ClipboardText, permission: 'commerce:write' },
  { label: '今日概览', path: '/', icon: GraduationCap },
  { label: '我的教学', path: '/workbench', icon: GraduationCap, permission: 'lesson:read' },
  { label: '教案与课件', path: '/teaching', icon: ChalkboardTeacher, permission: 'course:read' },
  { label: '班级教学计划', path: '/teaching/themes', icon: Calendar, permission: 'course:read' },
  { label: '作品与课效', path: '/portfolio', icon: ClipboardText, permission: 'student:read' },
  { label: '成长与作品展', path: '/growth', icon: ClipboardText, permission: 'student:read' },
  { label: '家庭授权', path: '/families', icon: UsersThree, permission: 'student:read' },
  { label: '消息与待办', path: '/messages', icon: ClipboardText, permission: 'lesson:read' },
  { label: '学员档案', path: '/students', icon: UsersThree, permission: 'student:read' },
  { label: '课程记录', path: '/lesson-history', icon: ClipboardText, permission: 'student:read' },
  {
    label: '老师可用时段',
    path: '/teachers/availabilities',
    icon: ChalkboardTeacher,
    permission: 'teacher:availability:read',
  },
  { label: '课程', path: '/courses', icon: ChalkboardTeacher, permission: 'course:read' },
  {
    label: '分组标签',
    path: '/courses/class-groups',
    icon: UsersThree,
    permission: 'classgroup:read',
  },
  { label: '周课表', path: '/schedule', icon: Calendar, permission: 'lesson:read' },
  { label: '课堂签到', path: '/attendance', icon: ClipboardText, permission: 'attendance:read' },
  { label: '请假审批', path: '/attendance/leaves', icon: ClipboardText, permission: 'leave:read' },
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
