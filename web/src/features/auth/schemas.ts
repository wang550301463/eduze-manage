import { z } from 'zod';

export const loginSchema = z.object({
  username: z
    .string()
    .min(4, '用户名至少 4 个字符')
    .max(32, '用户名最多 32 个字符'),
  // Authenticate existing credentials; password creation rules belong to the identity service.
  password: z.string().min(1, '请输入密码'),
  remember: z.boolean().optional(),
});

export type LoginFormValues = z.infer<typeof loginSchema>;
