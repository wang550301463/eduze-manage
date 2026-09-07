export type DayPeriod = 'morning' | 'afternoon' | 'evening';

export type RosterStatus = 'not_arrived' | 'checked_in' | 'checked_out' | 'absent' | 'leave';

export type RosterItem = {
  lessonId: string | number;
  studentId: string | number;
  studentName: string;
  classGroupName: string;
  lessonStartAt: string;
  attendanceId: string | number | null;
  status: number | null;
  statusLabel: string;
  checkInAt: string | null;
  checkOutAt: string | null;
};

export type TodayRoster = {
  items: RosterItem[];
  totalExpected: number;
  checkedInCount: number;
};

export type AttendanceRecord = {
  id: number;
  lessonId: number;
  studentId: number;
  studentName: string;
  classGroupName: string;
  status: number;
  statusLabel: string;
  checkInAt: string | null;
  checkOutAt: string | null;
  checkInMethod: string | null;
  note: string | null;
  lessonStartAt: string;
  lessonEndAt: string;
};

export type GuardianSummary = {
  id: string;
  name: string;
  phone: string;
  relation: string;
  canPickup: number;
  isMainContact: number;
};

export type LeaveRecord = {
  id: string | number;
  branchId: string | number;
  studentId: string | number;
  studentName: string;
  lessonId: string | number | null;
  leaveStartDate: string;
  leaveEndDate: string;
  reason: string | null;
  status: number;
  statusLabel: string;
  approvedBy: string | number | null;
  approvedAt: string | null;
  createdAt: string;
};

export type StudentAttendanceStat = {
  total: number;
  present: number;
  absent: number;
  leave: number;
  rate: number;
};

export type BranchAttendanceStat = {
  branchId: number;
  total: number;
  present: number;
  absent: number;
  leave: number;
  rate: number;
  classGroups?: ClassGroupAttendanceStat[];
};

export type ClassGroupAttendanceStat = {
  classGroupId: number;
  classGroupName: string;
  total: number;
  present: number;
  absent: number;
  leave: number;
  rate: number;
};

export type DashboardKpis = {
  weekAbsent: number;
  monthAttendanceRate: number;
  weekNewStudents: number;
  weekLessons: number;
};

export function rosterVisualStatus(status: number | null): RosterStatus {
  if (status === 2) return 'checked_in';
  if (status === 3) return 'checked_out';
  if (status === 4) return 'absent';
  if (status === 5) return 'leave';
  return 'not_arrived';
}
