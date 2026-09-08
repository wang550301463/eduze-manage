export type Role = "TEACHER" | "PARENT" | "VISITOR";
export type {
  IdentityUser as MiniUser,
  IdentityLogin as MiniLogin,
} from "../../packages/contracts";
import type {
  IdentityUser as MiniUser,
  IdentityLogin as MiniLogin,
} from "../../packages/contracts";
export interface Scope {
  role: Role;
  childId: string;
  branchId: string;
}
export class Session {
  constructor(private onClear: () => void = () => {}) {}
  token = "";
  user: MiniUser | null = null;
  scope: Scope = { role: "VISITOR", childId: "", branchId: "" };
  revision = 0;
  cache = new Map<string, unknown>();
  signIn(login: MiniLogin) {
    this.clear();
    this.token = login.accessToken;
    this.user = login.user;
    this.scope = {
      role: login.user.roles.some((r) =>
        ["TEACHER", "SUPER_ADMIN", "ADMIN", "PRINCIPAL"].includes(r),
      )
        ? "TEACHER"
        : "PARENT",
      childId: "",
      branchId: String(login.user.branches[0]?.id ?? ""),
    };
  }
  switchScope(scope: Partial<Scope>) {
    const next = { ...this.scope, ...scope };
    if (JSON.stringify(next) !== JSON.stringify(this.scope)) {
      this.revision++;
      this.cache.clear();
      this.scope = next;
    }
  }
  clear() {
    this.onClear();
    this.token = "";
    this.user = null;
    this.scope = { role: "VISITOR", childId: "", branchId: "" };
    this.revision++;
    this.cache.clear();
  }
}
export class RequestError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
  }
}
export type Transport = (options: {
  path: string;
  method: string;
  data?: unknown;
  headers: Record<string, string>;
}) => Promise<unknown>;
export class MiniClient {
  constructor(
    private session: Session,
    private transport: Transport,
  ) {}
  async request<T>(path: string, method = "GET", data?: unknown): Promise<T> {
    const revision = this.session.revision;
    const result = (await this.transport({
      path,
      method,
      data,
      headers: this.session.token
        ? { Authorization: `Bearer ${this.session.token}` }
        : {},
    })) as {
      statusCode: number;
      data: { code: number; message?: string; data: T };
    };
    if (revision !== this.session.revision)
      throw new RequestError(409, "身份已切换，请重新打开页面");
    if (result.statusCode === 401) this.session.clear();
    if (
      result.statusCode < 200 ||
      result.statusCode >= 300 ||
      result.data.code !== 0
    )
      throw new RequestError(
        result.statusCode,
        result.statusCode === 409 &&
          method === "PUT" &&
          /\/v1\/(portfolio|teaching)\//.test(path)
          ? "其他设备已更新草稿，请保留文字并重新打开最新版本"
          : (result.data.message ?? "网络或服务异常，请重试"),
      );
    return result.data.data;
  }
  get<T>(path: string) {
    return this.request<T>(path);
  }
  post<T>(path: string, data: unknown, method = "POST") {
    return this.request<T>(path, method, data);
  }
}
