import { WarningCircle } from '@phosphor-icons/react';

export function CameraPermissionAlert(): JSX.Element {
  return (
    <div className="flex items-start gap-3 rounded-lg border border-warning/30 bg-warning/10 p-4 text-sm">
      <WarningCircle className="mt-0.5 h-5 w-5 shrink-0 text-warning" weight="fill" />
      <div>
        <p className="font-medium text-foreground">无法访问摄像头</p>
        <p className="mt-1 text-muted-fg">
          请在浏览器设置中允许本站点使用摄像头，或改用手动签到。
        </p>
      </div>
    </div>
  );
}
