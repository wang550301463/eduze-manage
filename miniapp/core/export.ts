import { api } from "./runtime";
export async function exportSvg(
  path: string,
): Promise<{ filePath: string; fileName: string }> {
  const result = await api.get<{ svg: string; filename: string }>(path);
  if (!result.svg?.includes("<svg"))
    throw new Error("导出文件暂不可用，请重试");
  const fileName =
    result.filename.replace(/[^a-zA-Z0-9._-]/g, "_").replace(/^\.+/, "") ||
    "studio.svg";
  const filePath = `${wx.env.USER_DATA_PATH}/${Date.now()}-${fileName}`;
  await new Promise<void>((resolve, reject) =>
    wx.getFileSystemManager().writeFile({
      filePath,
      data: result.svg,
      encoding: "utf8",
      success: resolve,
      fail: reject,
    }),
  );
  return { filePath, fileName };
}
export async function shareExport(
  filePath: string,
  fileName: string,
): Promise<void> {
  await new Promise<void>((resolve, reject) =>
    wx.shareFileMessage({ filePath, fileName, success: resolve, fail: reject }),
  );
}
