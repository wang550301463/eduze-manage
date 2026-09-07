export type Lesson = {
  id: number;
  branchId: number;
  classGroupId: number;
  classGroupName?: string;
  courseId?: number;
  courseName?: string;
  classRoomId?: number;
  classRoomName?: string;
  teacherId?: number;
  teacherName?: string;
  startAt: string;
  endAt: string;
  status: string;
  note?: string;
};

export type ScheduleLessonItem = {
  id: number;
  classGroupId: number;
  classGroupName: string;
  courseId?: number;
  courseName: string;
  teacherId?: number;
  teacherShortName: string;
  classRoomId?: number;
  classRoomShortName: string;
  startAt: string;
  endAt: string;
  status: string;
  color: string;
};

export type WeekSchedule = {
  weekStart: string;
  weekEnd: string;
  days: { date: string; lessons: ScheduleLessonItem[] }[];
};

export type ConflictReport = {
  hasConflict: boolean;
  teacher?: Lesson;
  classRoom?: Lesson;
  classGroup?: Lesson;
};

export type LessonChangeLog = {
  id: number;
  changeType: string;
  beforeJson?: string;
  afterJson?: string;
  reason?: string;
  createdAt: string;
};

export type BulkGenerateResult = {
  generated: number;
  conflicts: { date: string; reason: string }[];
};
