export type Course = {
  /** 后端 Long 以字符串返回，避免 JS 精度丢失 */
  id: string;
  tenantId: string;
  name: string;
  ageMin?: number;
  ageMax?: number;
  lessonMinutes?: number;
  coverUrl?: string;
  description?: string;
};

export type ClassGroup = {
  id: string | number;
  tenantId: string | number;
  branchId: string | number;
  name: string;
  courseId?: string | number;
  courseName?: string;
  headTeacherId?: string | number;
  headTeacherName?: string;
  capacity: number;
  currentCount: number;
  status: number;
};

export type ClassRoom = {
  id: number;
  tenantId: number;
  branchId: number;
  name: string;
  capacity?: number;
  note?: string;
};

export type ClassMember = {
  studentId: string | number;
  studentName?: string;
  enrollNo?: string;
  joinedAt?: string;
  leftAt?: string;
};

export type PageResult<T> = {
  items: T[];
  total: number;
  page: number;
  size: number;
};
