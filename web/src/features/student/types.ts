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

export type PageResult<T> = {
  records: T[];
  total: number;
  page: number;
  size: number;
};

/** 后端 Long 序列化为 string，避免 JS 精度丢失 */
export type Branch = { id: string; name: string; code: string };
