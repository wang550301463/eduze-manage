import { BrowserQRCodeReader, type IScannerControls } from '@zxing/browser';
import type { Exception, Result } from '@zxing/library';
import { useEffect, useRef, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DialogFrame } from '@/components/ui/Dialog';
import { CameraPermissionAlert } from '@/features/attendance/components/CameraPermissionAlert';
import { checkIn } from '@/features/attendance/api';
import { toast } from '@/lib/toast';

type Props = {
  open: boolean;
  onClose: () => void;
  lessonId?: number;
  studentId?: number;
};

export function QrScanDialog({ open, onClose, lessonId, studentId }: Props): JSX.Element {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [cameraError, setCameraError] = useState(false);
  const [scanning, setScanning] = useState(false);

  useEffect(() => {
    if (!open) return undefined;
    const reader = new BrowserQRCodeReader();
    let controls: IScannerControls | undefined;
    let cancelled = false;

    (async () => {
      try {
        setCameraError(false);
        setScanning(true);
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: 'environment' },
        });
        if (cancelled) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play();
        }
        controls = await reader.decodeFromVideoDevice(
          undefined,
          videoRef.current!,
          async (result: Result | undefined, err: Exception | undefined) => {
            if (!result || err) return;
            const text = result.getText();
            try {
              await checkIn({
                method: 'qr',
                qrCode: text,
                ...(lessonId && studentId ? { lessonId, studentId } : {}),
              });
              toast.success('扫码签到成功');
            } catch (e) {
              toast.error(e instanceof Error ? e.message : '签到失败');
            }
          },
        );
      } catch {
        setCameraError(true);
      } finally {
        setScanning(false);
      }
    })();

    return () => {
      cancelled = true;
      controls?.stop();
      const video = videoRef.current;
      const stream = video?.srcObject as MediaStream | null;
      stream?.getTracks().forEach((t) => t.stop());
      if (video) video.srcObject = null;
    };
  }, [open, lessonId, studentId]);

  return (
    <DialogFrame
      open={open}
      onOpenChange={(v) => !v && onClose()}
      title="扫码签到"
      description="对准家长二维码，识别后自动签到"
      footer={<Button onClick={onClose}>关闭</Button>}
    >
      {cameraError ? <CameraPermissionAlert /> : null}
      <div className="relative aspect-video overflow-hidden rounded-lg bg-muted">
        <video ref={videoRef} className="h-full w-full object-cover" muted playsInline />
        {scanning ? (
          <p className="absolute bottom-2 left-0 right-0 text-center text-xs text-white drop-shadow">
            扫描中…
          </p>
        ) : null}
      </div>
      {!lessonId || !studentId ? (
        <p className="mt-2 text-xs text-muted-fg">
          提示：请先在花名册选择学员行，再打开扫码（完整连续扫码流程待课次上下文接通）。
        </p>
      ) : null}
    </DialogFrame>
  );
}
