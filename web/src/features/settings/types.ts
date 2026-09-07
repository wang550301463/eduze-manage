export type Branch = {
  id: number;
  name: string;
  code: string;
  address?: string;
  phone?: string;
  status?: number;
};

export type UserAccount = {
  id: number;
  username: string;
  name: string;
  phone?: string;
  email?: string;
  status?: number;
  roles?: string[];
  branchIds?: number[];
};

export type Role = {
  id: number;
  code: string;
  name: string;
  isBuiltin?: number;
};

export type PageResult<T> = {
  items: T[];
  total: number;
  page: number;
  size: number;
};
