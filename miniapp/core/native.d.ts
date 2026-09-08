interface WxFailure {
  errMsg: string;
}
interface WxRequest {
  url: string;
  method?: string;
  data?: unknown;
  header?: Record<string, string>;
  timeout?: number;
  success: (value: { statusCode: number; data: unknown }) => void;
  fail: (error: WxFailure) => void;
}
declare const wx: {
  request(options: WxRequest): void;
  getRecorderManager(): import("./audio").Recorder;
  authorize(options: {
    scope: string;
    success: () => void;
    fail: (e: WxFailure) => void;
  }): void;
  requestPayment(options: {
    timeStamp: string;
    nonceStr: string;
    package: string;
    signType: "RSA";
    paySign: string;
    success: () => void;
    fail: (error: WxFailure) => void;
  }): void;
  makePhoneCall(options: { phoneNumber: string }): void;
  setClipboardData(options: {
    data: string;
    success?: () => void;
    fail?: (e: WxFailure) => void;
  }): void;
  env: { USER_DATA_PATH: string };
  shareFileMessage(options: {
    filePath: string;
    fileName?: string;
    success: () => void;
    fail: (e: WxFailure) => void;
  }): void;
  login(options: {
    success: (v: { code: string }) => void;
    fail: (e: WxFailure) => void;
  }): void;
  getStorageSync(key: string): unknown;
  setStorageSync(key: string, value: unknown): void;
  removeStorageSync(key: string): void;
  navigateTo(o: { url: string }): void;
  reLaunch(o: { url: string }): void;
  showToast(o: { title: string; icon: "none" | "success" }): void;
  chooseMedia(o: {
    count: number;
    mediaType: string[];
    sourceType: string[];
    success: (v: {
      tempFiles: { tempFilePath: string; size: number; fileType: string }[];
    }) => void;
    fail: (e: WxFailure) => void;
  }): void;
  getFileSystemManager(): {
    writeFile(o: {
      filePath: string;
      data: string;
      encoding: "utf8";
      success: () => void;
      fail: (e: WxFailure) => void;
    }): void;
    readFile(o: {
      filePath: string;
      success: (v: { data: ArrayBuffer }) => void;
      fail: (e: WxFailure) => void;
    }): void;
  };
  previewImage(o: { urls: string[]; current?: string }): void;
  requestSubscribeMessage(o: {
    tmplIds: string[];
    success: (r: Record<string, string>) => void;
    fail: (e: WxFailure) => void;
  }): void;
};
interface MiniEvent {
  detail: { value: string };
  currentTarget: { dataset: Record<string, string> };
}
type PageOptions<T> = T & { data: Record<string, unknown> } & ThisType<
    T & { setData(data: Record<string, unknown>): void }
  >;
declare function Page<T>(options: PageOptions<T>): void;
declare function App(options: { onLaunch?: () => void }): void;
