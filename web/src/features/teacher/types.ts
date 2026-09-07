export type EntityId = string;

export type Teacher = {
  id: EntityId;
  username: string;
  name: string;
  branchId?: EntityId;
};

export type TeacherAvailability = {
  id: EntityId;
  teacherId: EntityId;
  branchId: EntityId;
  dayOfWeek: number;
  startMinute: number;
  endMinute: number;
  capacity: number;
  defaultClassRoomId?: EntityId;
  validFrom: string;
  validTo?: string;
  status: number;
  note?: string;
};

export type TeacherSchedule = {
  weekStart: string;
  columns: TeacherColumn[];
};

export type TeacherColumn = {
  teacherId: EntityId;
  teacherName: string;
  branchId?: EntityId;
  lessons: LessonCell[];
};

export type LessonCell = {
  lessonId: EntityId;
  classRoomId?: EntityId;
  classRoomName?: string;
  startAt: string;
  endAt: string;
  dayOfWeek: number;
  startMinute: number;
  endMinute: number;
  capacity?: number;
  studentCount: number;
  source?: number;
  teacherAvailabilityId?: EntityId;
  status: string;
};

export type Subscription = {
  id: EntityId;
  studentId: EntityId;
  studentName?: string;
  teacherId: EntityId;
  teacherName?: string;
  teacherAvailabilityId: EntityId;
  branchId: EntityId;
  dayOfWeek?: number;
  startMinute?: number;
  endMinute?: number;
  validFrom: string;
  validTo?: string;
  status: number;
  source: string;
  note?: string;
};

export type LessonStudentRow = {
  id: EntityId;
  lessonId: EntityId;
  studentId: EntityId;
  studentName?: string;
  subscriptionId?: EntityId;
  source: string;
  status: string;
  note?: string;
};
