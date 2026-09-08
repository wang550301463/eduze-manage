export interface RecordingFile {
  tempFilePath: string;
  fileSize: number;
}
export interface Recorder {
  start(options: { format: "mp3"; duration: number }): void;
  stop(): void;
  onStop(callback: (file: RecordingFile) => void): void;
  offStop(callback: (file: RecordingFile) => void): void;
  onError(callback: (error: { errMsg: string }) => void): void;
  offError(callback: (error: { errMsg: string }) => void): void;
}
export class AudioCapture {
  private active = false;
  private revision = 0;
  private readonly stopped = (file: RecordingFile) => {
    if (!this.active) return;
    this.active = false;
    if (this.revision === this.scope()) this.done(file);
  };
  private readonly failed = (error: { errMsg: string }) => {
    if (!this.active) return;
    this.active = false;
    if (this.revision === this.scope()) this.error(error.errMsg);
  };
  constructor(
    private recorder: Recorder,
    private scope: () => number,
    private done: (file: RecordingFile) => void,
    private error: (message: string) => void,
  ) {
    recorder.onStop(this.stopped);
    recorder.onError(this.failed);
  }
  start() {
    if (this.active) return;
    this.revision = this.scope();
    this.active = true;
    this.recorder.start({ format: "mp3", duration: 60000 });
  }
  stop() {
    if (this.active) this.recorder.stop();
  }
  cancel() {
    this.active = false;
    this.recorder.stop();
  }
  dispose() {
    this.cancel();
    this.recorder.offStop(this.stopped);
    this.recorder.offError(this.failed);
  }
}
