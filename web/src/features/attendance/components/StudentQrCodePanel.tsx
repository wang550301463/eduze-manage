import { QRCodeSVG } from 'qrcode.react';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { generateGuardianQr } from '@/features/attendance/api';
import { toast } from '@/lib/toast';

type Props = {
  guardianId: number | string;
  guardianName: string;
};

export function StudentQrCodePanel({ guardianId, guardianName }: Props): JSX.Element {
  const [qrCode, setQrCode] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const res = await generateGuardianQr(guardianId);
      setQrCode(res.qrCode);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '生成失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col items-center gap-3 rounded-lg border border-border p-4">
      <p className="text-sm font-medium">{guardianName}</p>
      {qrCode ? (
        <QRCodeSVG value={qrCode} size={160} level="M" />
      ) : (
        <div className="flex h-40 w-40 items-center justify-center rounded bg-muted text-xs text-muted-fg">
          未生成
        </div>
      )}
      <Button size="sm" variant="secondary" onClick={load} disabled={loading}>
        {qrCode ? '重新生成' : '显示二维码'}
      </Button>
    </div>
  );
}
