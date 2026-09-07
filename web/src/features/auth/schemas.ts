import { z } from 'zod';

export const loginSchema = z.object({
  username: z
    .string()
    .min(4, '用户名至少 4 个字符')
    .max(32, '用户名最多 32 个字符'),
  password: z
    .string()
    .min(6, '密码至少 6 个字符')
    .max(32, '密码最多 32 个字符'),
  remember: z.boolean().optional(),
});

export type LoginFormValues = z.infer<typeof loginSchema>;
