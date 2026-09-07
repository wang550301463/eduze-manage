import { zodResolver } from '@hookform/resolvers/zod';
import { isAxiosError } from 'axios';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Navigate, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Checkbox } from '@/components/ui/Checkbox';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { login } from '@/features/auth/api';
import { loginSchema, type LoginFormValues } from '@/features/auth/schemas';
import { isAuthenticated, useAuthStore } from '@/features/auth/store';
import { ApiError, ErrorCodes } from '@/lib/api-types';
import { toast } from '@/lib/toast';

function BrandLogo(): JSX.Element {
  return (
    <svg viewBox="0 0 48 48" className="mx-auto h-12 w-12" aria-hidden role="img">
      <circle cx="24" cy="24" r="22" fill="hsl(var(--primary))" opacity="0.15" />
      <path
        d="M14 32 L24 14 L34 32 Z"
        fill="hsl(var(--primary))"
        stroke="hsl(var(--primary))"
        strokeWidth="1"
      />
    </svg>
  );
}

function resolveLoginError(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.code === ErrorCodes.ACCOUNT_LOCKED) {
      return '账号已锁定，请 10 分钟后再试';
    }
    if (err.code === ErrorCodes.TOO_MANY_REQUESTS) {
      return '登录尝试过于频繁，请稍后再试';
    }
    return err.message || '账号或密码错误';
  }
  if (isAxiosError(err)) {
    const code = err.response?.data?.code as number | undefined;
    const message = err.response?.data?.message as string | undefined;
    if (code === ErrorCodes.ACCOUNT_LOCKED) {
      return '账号已锁定，请 10 分钟后再试';
    }
    if (code === ErrorCodes.TOO_MANY_REQUESTS) {
      return '登录尝试过于频繁，请稍后再试';
    }
    return message ?? '账号或密码错误';
  }
  return '登录失败，请稍后再试';
}

export function LoginPage(): JSX.Element {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
    setValue,
    watch,
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: '', password: '', remember: true },
  });

  const remember = watch('remember');

  if (isAuthenticated()) {
    return <Navigate to="/" replace />;
  }

  const onSubmit = handleSubmit(async (values) => {
    setSubmitting(true);
    try {
      const result = await login({
        username: values.username.trim(),
        password: values.password,
      });
      setAuth({
        accessToken: result.accessToken,
        refreshToken: result.refreshToken,
        user: result.user,
      });
      toast.success(`欢迎回来，${result.user.name}`);
      navigate('/', { replace: true });
    } catch (err) {
      toast.error(resolveLoginError(err));
    } finally {
      setSubmitting(false);
    }
  });

  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-6">
      <div className="w-full max-w-sm space-y-6 rounded-xl border border-border bg-background p-8 shadow-lg">
        <div className="text-center">
          <BrandLogo />
          <h1 className="mt-3 font-serif text-2xl font-bold text-primary">EduZE Manage</h1>
          <p className="mt-1 text-sm text-muted-fg">机构后台管理系统</p>
        </div>
        <form className="space-y-4" onSubmit={onSubmit} aria-label="登录">
          <div>
            <Label htmlFor="username">账号</Label>
            <Input
              id="username"
              type="text"
              autoComplete="username"
              placeholder="手机号 / 邮箱"
              className="mt-1"
              {...register('username')}
            />
            {errors.username ? (
              <p className="mt-1 text-xs text-destructive">{errors.username.message}</p>
            ) : null}
          </div>
          <div>
            <Label htmlFor="password">密码</Label>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              placeholder="密码"
              className="mt-1"
              {...register('password')}
            />
            {errors.password ? (
              <p className="mt-1 text-xs text-destructive">{errors.password.message}</p>
            ) : null}
          </div>
          <div className="flex items-center gap-2">
            <Checkbox
              id="remember"
              checked={remember === true}
              onCheckedChange={(v) => setValue('remember', v === true)}
              aria-label="记住我"
            />
            <Label htmlFor="remember" className="font-normal">
              记住我
            </Label>
          </div>
          <Button type="submit" className="w-full" disabled={submitting}>
            {submitting ? '登录中…' : '登录'}
          </Button>
        </form>
      </div>
    </div>
  );
}
