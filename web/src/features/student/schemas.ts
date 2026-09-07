import { z } from 'zod';

/** 第一期：电话可留空；填写时仅校验长度，不强制 11 位手机号 */
const optionalPhoneLoose = (label: string) =>
  z
    .string()
    .optional()
    .transform((v) => (v ?? '').trim())
    .refine((v) => v === '' || (v.length >= 7 && v.length <= 20), {
      message: `${label}须为空或 7–20 位`,
    });

export const studentFormSchema = z.object({
  branchId: z.coerce.string().min(1, '请选择校区'),
  enrollNo: z.string().trim().min(1, '请填写入园编号').max(64, '入园编号过长'),
  name: z.string().trim().min(1, '请填写姓名').max(64),
  gender: z.coerce.number().min(0).max(2).default(0),
  birthday: z.string().optional(),
  enrollDate: z.string().optional(),
  allergy: z.string().max(512).optional(),
  healthNote: z.string().max(1024).optional(),
  emergencyContact: z.string().max(128).optional(),
  emergencyPhone: optionalPhoneLoose('紧急联系电话'),
  mentorTeacherId: z.coerce.string().min(1, '请选择主带老师'),
  currentStageId: z.coerce.string().optional(),
  guardianName: z.string().max(64).optional(),
  guardianPhone: optionalPhoneLoose('家长手机号'),
  guardianRelation: z.string().max(32).optional(),
  skipClass: z.boolean().default(true),
});

export type StudentFormValues = z.infer<typeof studentFormSchema>;
