export type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
  traceId?: string;
};

export class ApiError extends Error {
  constructor(
    public readonly code: number,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/** 与后端 ErrorCode 对齐的业务码 */
export const ErrorCodes = {
  OK: 0,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  ACCOUNT_LOCKED: 42301,
  TOO_MANY_REQUESTS: 42900,
} as const;
