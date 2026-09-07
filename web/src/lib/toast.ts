import { toast as sonnerToast } from 'sonner';

export const toast = {
  success: (message: string) => sonnerToast.success(message),
  error: (message: string) => sonnerToast.error(message),
  info: (message: string) => sonnerToast.info(message),
  undo: ({ message, onUndo }: { message: string; onUndo: () => void }) =>
    sonnerToast(message, {
      action: {
        label: '撤销',
        onClick: onUndo,
      },
      duration: 4200,
    }),
};
