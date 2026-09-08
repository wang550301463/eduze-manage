import { useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { isAxiosError } from 'axios';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Navigate, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Brand } from '@/app/shell/Brand';
import { StudioArtwork } from '@/features/dashboard/components/StudioArtwork';
import { ArrowRight } from '@phosphor-icons/react';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { login } from '@/features/auth/api';
import { loginSchema, type LoginFormValues } from '@/features/auth/schemas';
import { isAuthenticated, useAuthStore } from '@/features/auth/store';
import { ApiError, ErrorCodes } from '@/lib/api-types';
import { toast } from '@/lib/toast';

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
  const queryClient = useQueryClient();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [submitting, setSubmitting] = useState(false);
  const [loginError, setLoginError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: '', password: '', remember: true },
  });

  if (isAuthenticated()) {
    return <Navigate to="/" replace />;
  }

  const onSubmit = handleSubmit(async (values) => {
    setSubmitting(true);
    setLoginError('');
    try {
      const result = await login({
        username: values.username.trim(),
        password: values.password,
      });
      // Prevent cached records from a previous account entering this session.
      queryClient.clear();
      setAuth({
        accessToken: result.accessToken,
        refreshToken: result.refreshToken,
        user: result.user,
      });
      toast.success(`欢迎回来，${result.user.name}`);
      navigate('/', { replace: true });
    } catch (err) {
      setLoginError(resolveLoginError(err));
    } finally {
      setSubmitting(false);
    }
  });

  return (
    <div className="studio-background flex min-h-dvh items-center justify-center p-4 sm:p-8">
      <div className="grid w-full max-w-[1020px] overflow-hidden rounded-[28px] border border-white/80 bg-white/50 shadow-[0_24px_90px_#35443e12] backdrop-blur-2xl md:min-h-[650px] md:grid-cols-[1.1fr_1fr]">
        <section className="relative hidden flex-col justify-between border-r border-white/70 bg-white/20 p-10 md:flex">
          <Brand />
          <div className="-my-5">
            <StudioArtwork large />
          </div>
          <div>
            <p className="studio-eyebrow mb-4">A LITTLE SPACE FOR BIG IDEAS</p>
            <h1 className="text-[32px] font-medium leading-snug tracking-tight">
              给每一份想象，
              <br />
              留一片自由。
            </h1>
            <p className="mt-4 max-w-xs text-[13px] leading-6 text-muted-fg">
              连接教学与成长，让老师从容教，
              <br />
              让孩子自在画。
            </p>
          </div>
        </section>
        <div className="flex flex-col justify-center bg-white/45 px-6 py-12 sm:px-12">
          <Brand className="mb-10 md:hidden" />
          <p className="studio-eyebrow mb-3">WELCOME BACK</p>
          <h2 className="text-[28px] font-semibold tracking-tight">欢迎回到美术宝</h2>
          <p className="mb-9 mt-3 text-[13px] leading-6 text-muted-fg">
            登录你的账号，开启今天的教学时光。
          </p>
          <form className="space-y-5" onSubmit={onSubmit} aria-label="登录">
            <div>
              <Label htmlFor="username">账号</Label>
              <Input
                id="username"
                autoComplete="username"
                placeholder="请输入机构账号"
                className="mt-2 h-11 bg-white/80"
                aria-invalid={!!errors.username}
                {...register('username')}
              />
              {errors.username && (
                <p className="mt-2 text-xs text-error">{errors.username.message}</p>
              )}
            </div>
            <div>
              <Label htmlFor="password">密码</Label>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                placeholder="请输入密码"
                className="mt-2 h-11 bg-white/80"
                aria-invalid={!!errors.password}
                {...register('password')}
              />
              {errors.password && (
                <p className="mt-2 text-xs text-error">{errors.password.message}</p>
              )}
            </div>
            {loginError && (
              <p role="alert" className="rounded-lg bg-error/5 px-3 py-2 text-sm text-error">
                {loginError}
              </p>
            )}
            <Button type="submit" className="h-11 w-full gap-2 shadow-sm" disabled={submitting}>
              {submitting ? '正在进入…' : '进入工作空间'}
              <ArrowRight className="h-4 w-4" />
            </Button>
          </form>
          <p className="mt-6 text-xs leading-6 text-muted-fg">
            账号由机构统一开通。如需帮助，请联系机构管理员。
          </p>
          <p className="mt-14 text-[10px] tracking-[.16em] text-muted-fg">美术宝 · ART STUDIO</p>
        </div>
      </div>
    </div>
  );
}
