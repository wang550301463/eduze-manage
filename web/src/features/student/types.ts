/** 与后端 Jackson Long→string 一致 */
export type EntityId = string;

export type ClassGroupRef = { id: EntityId; name: string };

export type Student = {
  id: EntityId;
  branchId: EntityId;
  branchName?: string;
  enrollNo: string;
  name: string;
  gender: number;
  birthday?: string;
  enrollDate?: string;
  status: number;
  allergy?: string;
  healthNote?: string;
  emergencyContact?: string;
  emergencyPhone?: string;
  avatarUrl?: string;
  mentorTeacherId?: EntityId;
  mentorTeacherName?: string;
  currentStageId?: EntityId;
  currentStageCode?: string;
  currentStageName?: string;
  classGroups: ClassGroupRef[];
  totalRemaining: number;
  alertLow: boolean;
};

export type TeacherSummary = {
  id: EntityId;
  username: string;
  name: string;
  branchId?: EntityId;
};

export type MentorHistoryItem = {
  id: EntityId;
  fromTeacherId?: EntityId;
  fromTeacherName?: string;
  toTeacherId: EntityId;
  toTeacherName?: string;
  reason?: string;
  changedAt: string;
};

export type StageAssessment = {
  id: EntityId;
  studentId: EntityId;
  stageId: EntityId;
  stageCode?: string;
  stageName?: string;
  assessedAt: string;
  assessedBy?: EntityId;
  scores?: Record<string, number>;
  comment?: string;
  createdAt: string;
};

export type Guardian = {
  id: EntityId;
  name: string;
  phone: string;
  isMainContact: number;
  canPickup: number;
  relation?: string;
};

export type CoursePackage = {
  id: EntityId;
  studentId: EntityId;
  branchId: EntityId;
  totalLessons: number;
  remainingLessons: number;
  expireDate?: string;
  note?: string;
  alertLow: boolean;
};

export type LessonHourLedger = {
  id: EntityId;
  studentId: EntityId;
  branchId: EntityId;
  lessonId?: EntityId;
  lessonStudentId?: EntityId;
  packageId?: EntityId;
  eventType: string;
  minutesDelta: number;
  balanceAfterMinutes?: number | null;
  occurredAt: string;
  operatorId?: EntityId;
  note?: string;
  relatedLedgerId?: EntityId;
  createdAt?: string;
};

/** 学员历史课程快照（审计） */
export type StudentLessonHistory = {
  id: EntityId;
  branchId?: EntityId;
  studentId: EntityId;
  studentName: string;
  lessonId: EntityId;
  attendanceId?: EntityId | null;
  courseId?: EntityId | null;
  courseName?: string | null;
  classGroupId?: EntityId | null;
  classGroupName?: string | null;
  teacherId?: EntityId | null;
  teacherName?: string | null;
  classRoomId?: EntityId | null;
  classRoomName?: string | null;
  startAt?: string | null;
  endAt?: string | null;
  source?: number | null;
  sourceLabel?: string;
  attendanceStatus?: number | null;
  minutes?: number | null;
  snapshotJson?: string | null;
  occurredAt: string;
  createdAt?: string;
};

export type PageResult<T> = {
  records: T[];
  total: number;
  page: number;
  size: number;
};

/** 后端 Long 序列化为 string，避免 JS 精度丢失 */
export type Branch = { id: string; name: string; code: string };
