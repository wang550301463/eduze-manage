import type { UploadTicket } from "../../packages/contracts";
import { MiniClient, Session, type MiniLogin } from "./client";
import { config } from "../config";
export const session = new Session(() => wx.removeStorageSync("eduze-session"));
export const api = new MiniClient(
  session,
  (options) =>
    new Promise((resolve, reject) => {
      if (!config.apiBase) {
        reject(new Error("尚未配置小程序 HTTPS 服务域名"));
        return;
      }
      wx.request({
        url: `${config.apiBase}/api${options.path}`,
        method: options.method,
        data: options.data,
        header: { "Content-Type": "application/json", ...options.headers },
        timeout: 15000,
        success: resolve,
        fail: (e) => reject(new Error(e.errMsg || "网络不可用，请重试")),
      });
    }),
);
export function restoreSession() {
  const saved = wx.getStorageSync("eduze-session") as MiniLogin | undefined;
  if (saved?.accessToken && saved?.user) applyLogin(saved);
}
export async function login() {
  const code = await new Promise<string>((resolve, reject) =>
    wx.login({ success: (r) => resolve(r.code), fail: reject }),
  );
  const result = await api.post<MiniLogin>("/v1/identity/wechat/login", {
    code,
  });
  applyLogin(result);
}
export function applyLogin(result: MiniLogin) {
  session.signIn(result);
  wx.setStorageSync("eduze-session", result);
}
export function logout() {
  session.clear();
  wx.removeStorageSync("eduze-session");
  wx.reLaunch({ url: "/pages/home/index" });
}
export function errorMessage(e: unknown) {
  return e instanceof Error ? e.message : "操作失败，请稍后再试";
}
export function open(url: string) {
  wx.navigateTo({ url });
}
export async function upload(
  path: string,
  size: number,
  kind: string,
): Promise<string> {
  const fileName = path.split("/").pop() ?? "image.jpg";
  const extension = fileName.split(".").pop()?.toLowerCase();
  const contentType =
    kind === "audio"
      ? "audio/mpeg"
      : kind === "video"
        ? "video/mp4"
        : extension === "png"
          ? "image/png"
          : extension === "webp"
            ? "image/webp"
            : "image/jpeg";
  const ticket = await api.post<UploadTicket>("/v1/media/uploads", {
    branchId: session.scope.branchId,
    purpose: "PORTFOLIO",
    fileName,
    contentType,
    size,
  });
  const data = await new Promise<ArrayBuffer>((resolve, reject) =>
    wx.getFileSystemManager().readFile({
      filePath: path,
      success: (r) => resolve(r.data),
      fail: reject,
    }),
  );
  await new Promise<void>((resolve, reject) =>
    wx.request({
      url: ticket.uploadUrl,
      method: ticket.method,
      data,
      header: ticket.headers,
      timeout: 60000,
      success: (r) =>
        r.statusCode >= 200 && r.statusCode < 300
          ? resolve()
          : reject(new Error("文件上传失败，请重试")),
      fail: reject,
    }),
  );
  await api.post(`/v1/media/uploads/${ticket.id}/complete`, {});
  return ticket.id;
}

export { viewLinks } from "./media";
