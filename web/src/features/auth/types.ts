export type BranchInfo = {
  id: number;
  name: string;
  code: string;
};

export type UserInfo = {
  id: number;
  name: string;
  username: string;
  roles: string[];
  permissions: string[];
  branches: BranchInfo[];
};

export type LoginResult = {
  accessToken: string;
  refreshToken: string;
  user: UserInfo;
};

export type LoginRequest = {
  username: string;
  password: string;
};
