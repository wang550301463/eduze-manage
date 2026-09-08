import {
  api,
  session,
  login,
  logout,
  applyLogin,
  errorMessage,
  open,
} from "../../core/runtime";
import type { Role, MiniLogin } from "../../core/client";
import type { Child } from "../../../packages/contracts";
Page({
  data: {
    loggedIn: false,
    name: "",
    role: "VISITOR",
    branches: [] as { id: string; name: string }[],
    branchName: "选择校区",
    children: [] as Child[],
    childName: "选择孩子",
    code: "",
    username: "",
    password: "",
    busy: false,
    error: "",
    success: "",
  },
  onShow() {
    this.setData({
      loggedIn: Boolean(session.token),
      name: session.user?.name ?? "",
      role: session.scope.role,
      branches: session.user?.branches ?? [],
      branchName:
        session.user?.branches.find(
          (b) => String(b.id) === session.scope.branchId,
        )?.name ?? "选择校区",
      error: "",
      success: "",
    });
    if (session.token) void this.loadChildren();
  },
  async loadChildren() {
    try {
      const children = await api.get<Child[]>("/v1/academic/family/children");
      this.setData({
        children,
        childName:
          children.find((c) => c.id === session.scope.childId)?.name ??
          "选择孩子",
      });
    } catch (e) {
      this.setData({ error: errorMessage(e) });
    }
  },
  async signIn() {
    this.setData({ busy: true, error: "" });
    try {
      await login();
      this.onShow();
    } catch (e) {
      this.setData({ error: errorMessage(e) });
    } finally {
      this.setData({ busy: false });
    }
  },
  signOut: logout,
  async role(e: MiniEvent) {
    const role = e.currentTarget.dataset.role as Role;
    this.setData({ busy: true, error: "" });
    try {
      if (role !== "VISITOR") {
        const result = await api.post<MiniLogin>("/v1/identity/switch-role", {
          role: role === "TEACHER" ? "STAFF" : "PARENT",
        });
        applyLogin(result);
      }
      session.switchScope({ role });
      this.onShow();
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  branch(e: MiniEvent) {
    const selected = this.data.branches[Number(e.detail.value)];
    if (selected) {
      session.switchScope({ branchId: String(selected.id), childId: "" });
      this.setData({ branchName: selected.name, childName: "选择孩子" });
    }
  },
  child(e: MiniEvent) {
    const selected = this.data.children[Number(e.detail.value)];
    if (selected) {
      session.switchScope({
        childId: selected.id,
        branchId: selected.branchId,
      });
      this.setData({ childName: selected.name });
    }
  },
  code(e: MiniEvent) {
    this.setData({ code: e.detail.value });
  },
  employeeField(e: MiniEvent) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value });
  },
  async linkEmployee() {
    this.setData({ busy: true, error: "", success: "" });
    try {
      const employee = await api.post<MiniLogin>("/auth/login", {
        username: this.data.username,
        password: this.data.password,
      });
      applyLogin(employee);
      const code = await new Promise<string>((resolve, reject) =>
        wx.login({ success: (r) => resolve(r.code), fail: reject }),
      );
      await api.post("/v1/identity/wechat/link", { code });
      await login();
      const staff = await api.post<MiniLogin>("/v1/identity/switch-role", {
        role: "STAFF",
      });
      applyLogin(staff);
      this.onShow();
      this.setData({ success: "员工账号已关联，可以从老师身份进入课堂" });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false, password: "" });
    }
  },
  async bind() {
    this.setData({ busy: true, error: "", success: "" });
    try {
      await api.post("/v1/academic/family/bindings", { code: this.data.code });
      this.setData({
        success: "申请已提交，请等待画室前台核验后开通",
        code: "",
        username: "",
        password: "",
      });
    } catch (e) {
      this.setData({ error: errorMessage(e) });
    } finally {
      this.setData({ busy: false });
    }
  },
  studio() {
    open("/pages/studio/index");
  },
  shop() {
    open("/pages/shop/index");
  },
  orders() {
    open("/pages/orders/index");
  },
  teacher() {
    open("/pages/teacher/index");
  },
  family() {
    if (!session.scope.childId) {
      this.setData({ error: "请先选择已授权的孩子" });
      return;
    }
    open("/pages/family/index");
  },
  messages() {
    open("/pages/messages/index");
  },
  exhibitions() {
    open("/pages/exhibitions/index");
  },
});
