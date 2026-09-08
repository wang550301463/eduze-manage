import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { listUsers } from '@/features/settings/api';
import { studentApi } from '@/features/student/api';
import { listClassGroups } from '@/features/course/api';
import { fetchWeekSchedule } from '@/features/lesson/api';
import { useAuthStore } from '@/features/auth/store';
import { command } from './client';
import { usePlatformList, useAction } from './hooks';
import {
  Panel,
  Field,
  fieldClass,
  buttonClass,
  secondaryClass,
  BranchPicker,
  Feedback,
  Notice,
} from './ui';
import type {
  Studio,
  Enquiry,
  Followup,
  Activity,
  Renewal,
} from '../../../../packages/contracts/business';
const leadLabels: Record<string, string> = {
  NEW: '新咨询',
  CONTACTED: '已联系',
  TRIAL_PENDING: '待安排试听',
  TRIAL_BOOKED: '已预约试听',
  ATTENDED: '已到店',
  ENROLLED: '已报名',
  LOST: '暂不报名',
};
export function EngagementPage() {
  const [branch, setBranch] = useState('');
  return (
    <div className="space-y-5">
      <div>
        <p className="text-xs tracking-[.2em] text-primary">WELCOME TO THE STUDIO</p>
        <h1 className="mt-2 text-2xl font-semibold">招生与画室经营</h1>
        <p className="mt-2 text-sm text-muted-fg">把一次作品分享，接续成咨询、体验和长期陪伴。</p>
      </div>
      <BranchPicker value={branch} onChange={setBranch} />
      {branch && <EngagementWorkspace key={branch} branchId={branch} />}
    </div>
  );
}
function StaffPicker({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  const user = useAuthStore((s) => s.user);
  const accounts = useQuery({
    queryKey: ['platform', user?.id, 'engagement-staff'],
    queryFn: () => listUsers(1, 100),
    enabled: Boolean(user?.permissions.includes('user:read')),
  });
  const items = accounts.data?.items ?? (user ? [user] : []);
  return (
    <Field label="跟进负责人">
      <select className={fieldClass} value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">选择负责人</option>
        {items.map((u) => (
          <option key={u.id} value={String(u.id)}>
            {u.name}
          </option>
        ))}
      </select>
    </Field>
  );
}
function EngagementWorkspace({ branchId }: { branchId: string }) {
  const [tab, setTab] = useState<'leads' | 'studio' | 'renewals' | 'dashboard'>('leads');
  const [selected, setSelected] = useState<Enquiry | null>(null);
  const leads = usePlatformList<Enquiry[]>(`/engagement/enquiries?branchId=${branchId}`);
  const [filter, setFilter] = useState('');
  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2">
        {[
          ['leads', '咨询与试听'],
          ['studio', '画室主页与活动'],
          ['renewals', '续费跟进'],
          ['dashboard', '来源与推荐'],
        ].map(([key, label]) => (
          <button
            key={key}
            className={tab === key ? buttonClass : secondaryClass}
            onClick={() => setTab(key as typeof tab)}
          >
            {label}
          </button>
        ))}
      </div>
      {tab === 'dashboard' ? (
        <EngagementDashboard branchId={branchId} />
      ) : tab === 'studio' ? (
        <StudioEditor branchId={branchId} />
      ) : tab === 'renewals' ? (
        <RenewalEditor branchId={branchId} />
      ) : (
        <>
          <Notice error={leads.error} />
          <select
            aria-label="线索状态"
            className={`${fieldClass} max-w-xs`}
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          >
            <option value="">全部咨询</option>
            {Object.entries(leadLabels).map(([value, label]) => (
              <option value={value} key={value}>
                {label}
              </option>
            ))}
          </select>
          {selected && (
            <LeadEditor key={selected.id} enquiry={selected} onClose={() => setSelected(null)} />
          )}
          <div className="grid gap-3 lg:grid-cols-2">
            {leads.data
              ?.filter((l) => !filter || l.status === filter)
              .map((l) => (
                <Panel key={l.id}>
                  <div className="flex justify-between">
                    <h2 className="font-semibold">
                      {l.studentName} · {l.age} 岁
                    </h2>
                    <span className="text-xs text-primary">{leadLabels[l.status]}</span>
                  </div>
                  <p className="mt-3 text-sm">联系电话：{l.phone}</p>
                  <p className="my-2 text-xs text-muted-fg">
                    来源：{l.source || '自主咨询'}
                    {l.sourceId ? ` · ${l.sourceId}` : ''}
                  </p>
                  <button className={secondaryClass} onClick={() => setSelected(l)}>
                    记录跟进 / 安排试听
                  </button>
                </Panel>
              ))}
          </div>
          {leads.data?.length === 0 && (
            <Panel>
              <p className="text-sm">
                还没有咨询。发布画室介绍和作品展，家长可以从小程序提交试听意向。
              </p>
            </Panel>
          )}
        </>
      )}
    </div>
  );
}
export function LeadEditor({ enquiry, onClose }: { enquiry: Enquiry; onClose: () => void }) {
  const [status, setStatus] = useState(enquiry.status);
  const [assignee, setAssignee] = useState(enquiry.assigneeId ?? '');
  const [note, setNote] = useState('');
  const [nextAt, setNextAt] = useState('');
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [session, setSession] = useState('');
  const [group, setGroup] = useState('');
  const [reserved, setReserved] = useState(Boolean(enquiry.reservationId));
  const user = useAuthStore((s) => s.user);
  const history = usePlatformList<Followup[]>(`/engagement/enquiries/${enquiry.id}/followups`);
  const schedule = useQuery({
    queryKey: ['platform', user?.id, 'trial-slots', enquiry.branchId, date],
    queryFn: () => fetchWeekSchedule(Number(enquiry.branchId), date),
  });
  const groups = useQuery({
    queryKey: ['platform', user?.id, 'enrollment-groups', enquiry.branchId],
    queryFn: () => listClassGroups(1, 100, enquiry.branchId),
  });
  const action = useAction();
  return (
    <Panel>
      <div className="flex justify-between">
        <h2 className="font-semibold">{enquiry.studentName}的跟进记录</h2>
        <button className={secondaryClass} onClick={onClose}>
          关闭
        </button>
      </div>
      <Feedback action={action} />
      <Notice error={history.error || schedule.error} />
      <div className="mt-3 grid gap-3 md:grid-cols-2">
        <StaffPicker value={assignee} onChange={setAssignee} />
        <Field label="跟进状态">
          <select
            disabled={enquiry.status === 'ENROLLED'}
            className={fieldClass}
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            {Object.entries(leadLabels)
              .filter(([v]) => v !== 'ENROLLED' || enquiry.status === 'ENROLLED')
              .map(([v, l]) => (
                <option value={v} key={v}>
                  {l}
                </option>
              ))}
          </select>
        </Field>
      </div>
      <Field label="沟通记录">
        <textarea className={fieldClass} value={note} onChange={(e) => setNote(e.target.value)} />
      </Field>
      <Field label="下次联系时间">
        <input
          type="datetime-local"
          className={fieldClass}
          value={nextAt}
          onChange={(e) => setNextAt(e.target.value)}
        />
      </Field>
      <button
        className={`${buttonClass} mt-3`}
        disabled={action.busy || !note.trim()}
        onClick={() =>
          void action.run(
            () =>
              command(`/engagement/enquiries/${enquiry.id}/followups`, {
                status,
                assigneeId: assignee,
                note,
                nextAt: nextAt ? new Date(nextAt).toISOString() : null,
              }),
            '跟进已记录',
          )
        }
      >
        保存跟进
      </button>
      <div className="mt-5 grid gap-3 border-t pt-4">
        <h3 className="font-semibold">安排真实试听课次</h3>
        {enquiry.status === 'ENROLLED' && (
          <p className="text-sm">该学生已经报名，后续服务请使用续费跟进。</p>
        )}
        <Field label="查看课堂日期">
          <input
            className={fieldClass}
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
          />
        </Field>
        <Field label="试听课堂">
          <select
            className={fieldClass}
            value={session}
            onChange={(e) => setSession(e.target.value)}
          >
            <option value="">选择可尝试预约的课次</option>
            {schedule.data?.days
              .flatMap((d) => d.lessons)
              .map((l) => (
                <option value={String(l.id)} key={l.id}>
                  {l.startAt.replace('T', ' ').slice(0, 16)} · {l.classGroupName} ·{' '}
                  {l.teacherShortName}
                </option>
              ))}
          </select>
        </Field>
        <button
          className={secondaryClass}
          disabled={!session || action.busy || enquiry.status === 'ENROLLED'}
          onClick={() =>
            void action.run(async () => {
              await command(`/engagement/enquiries/${enquiry.id}/trial`, { sessionId: session });
              setReserved(true);
            }, '试听名额已由教务确认')
          }
        >
          提交教务确认名额
        </button>
        <p className="text-xs text-muted-fg">
          实际报名状态由教务确认，不能通过手动跟进标记为已报名。
        </p>
        {reserved && enquiry.status !== 'ENROLLED' && (
          <>
            <Notice error={groups.error} />
            <Field label="正式报名班级">
              <select
                className={fieldClass}
                value={group}
                onChange={(e) => setGroup(e.target.value)}
              >
                <option value="">明确选择正式报名班级</option>
                {groups.data?.items.map((g) => (
                  <option key={g.id} value={String(g.id)}>
                    {g.name}
                  </option>
                ))}
              </select>
            </Field>
            <button
              className={buttonClass}
              disabled={action.busy || !group}
              onClick={() =>
                void action.run(async () => {
                  await command(`/engagement/enquiries/${enquiry.id}/enroll`, {
                    classGroupId: group,
                  });
                  onClose();
                }, '教务已确认正式报名')
              }
            >
              确认加入所选班级
            </button>
          </>
        )}
      </div>
      {history.data?.map((h, i) => (
        <div key={`${h.createdAt}-${i}`} className="mt-3 rounded-xl bg-white p-3">
          <p className="text-xs text-muted-fg">
            {new Date(h.createdAt).toLocaleString()} · {leadLabels[h.status]}
          </p>
          <p className="mt-2 whitespace-pre-wrap text-sm">{h.note}</p>
          {h.nextAt && (
            <p className="mt-1 text-xs">下次联系：{new Date(h.nextAt).toLocaleString()}</p>
          )}
        </div>
      ))}
    </Panel>
  );
}
function StudioEditor({ branchId }: { branchId: string }) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [address, setAddress] = useState('');
  const [phone, setPhone] = useState('');
  const [published, setPublished] = useState(true);
  const [activity, setActivity] = useState({
    title: '',
    description: '',
    startsAt: '',
    capacity: 20,
    published: true,
  });
  const studios = usePlatformList<Studio[]>('/engagement/public/studios');
  const activities = usePlatformList<Activity[]>(
    `/engagement/public/activities?branchId=${branchId}`,
  );
  const action = useAction();
  return (
    <div className="space-y-4">
      <Panel>
        <div className="flex justify-between">
          <h2 className="font-semibold">画室主页</h2>
          <button
            className={secondaryClass}
            onClick={() => {
              const s = studios.data?.find((s) => s.branchId === branchId);
              if (s) {
                setName(s.name);
                setDescription(s.description);
                setAddress(s.address);
                setPhone(s.phone);
              }
            }}
          >
            载入已发布资料
          </button>
        </div>
        <Notice error={studios.error || activities.error} />
        <Feedback action={action} />
        <form
          className="mt-3 grid gap-3"
          onSubmit={(e) => {
            e.preventDefault();
            void action.run(
              () =>
                command('/engagement/studios', {
                  branchId,
                  name,
                  description,
                  address,
                  phone,
                  published,
                }),
              '画室资料已保存',
            );
          }}
        >
          <Field label="画室名称">
            <input
              required
              className={fieldClass}
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </Field>
          <Field label="画室介绍">
            <textarea
              required
              className={fieldClass}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </Field>
          <Field label="地址">
            <input
              required
              className={fieldClass}
              value={address}
              onChange={(e) => setAddress(e.target.value)}
            />
          </Field>
          <Field label="咨询电话">
            <input
              required
              className={fieldClass}
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />
          </Field>
          <label className="text-sm">
            <input
              type="checkbox"
              checked={published}
              onChange={(e) => setPublished(e.target.checked)}
            />{' '}
            对外展示画室主页
          </label>
          <button className={buttonClass} disabled={action.busy}>
            保存画室资料
          </button>
        </form>
      </Panel>
      <Panel>
        <h2 className="font-semibold">免费开放活动</h2>
        <form
          className="mt-3 grid gap-3"
          onSubmit={(e) => {
            e.preventDefault();
            void action.run(
              () =>
                command('/engagement/activities', {
                  ...activity,
                  branchId,
                  startsAt: new Date(activity.startsAt).toISOString(),
                }),
              '活动已创建',
            );
          }}
        >
          <Field label="活动名称">
            <input
              required
              className={fieldClass}
              value={activity.title}
              onChange={(e) => setActivity({ ...activity, title: e.target.value })}
            />
          </Field>
          <Field label="活动介绍">
            <textarea
              required
              className={fieldClass}
              value={activity.description}
              onChange={(e) => setActivity({ ...activity, description: e.target.value })}
            />
          </Field>
          <Field label="活动时间">
            <input
              required
              type="datetime-local"
              className={fieldClass}
              value={activity.startsAt}
              onChange={(e) => setActivity({ ...activity, startsAt: e.target.value })}
            />
          </Field>
          <Field label="报名名额">
            <input
              required
              type="number"
              min="1"
              className={fieldClass}
              value={activity.capacity}
              onChange={(e) => setActivity({ ...activity, capacity: Number(e.target.value) })}
            />
          </Field>
          <button className={buttonClass} disabled={action.busy}>
            创建并发布活动
          </button>
          <p className="text-xs text-muted-fg">
            收费活动请在商城建立活动商品，以购买门票和核销管理名额。
          </p>
        </form>
      </Panel>
      {activities.data?.map((a) => (
        <Panel key={a.id}>
          <h3 className="font-semibold">{a.title}</h3>
          <p className="my-2 text-sm">
            {new Date(a.startsAt).toLocaleString()} · 已报名 {a.reserved} / {a.capacity}
          </p>
          <p className="text-sm text-muted-fg">{a.description}</p>
          <ActivityCheckin activityId={a.id} />
        </Panel>
      ))}
    </div>
  );
}
function RenewalEditor({ branchId }: { branchId: string }) {
  const [keyword, setKeyword] = useState('');
  const [student, setStudent] = useState('');
  const [assignee, setAssignee] = useState('');
  const [note, setNote] = useState('');
  const [nextAt, setNextAt] = useState('');
  const user = useAuthStore((s) => s.user);
  const students = useQuery({
    queryKey: ['platform', user?.id, 'renewal-students', branchId, keyword],
    queryFn: () => studentApi.list({ branchId, keyword, size: '50' }),
  });
  const renewals = usePlatformList<Renewal[]>(`/engagement/renewals?branchId=${branchId}`);
  const action = useAction();
  return (
    <>
      <Panel>
        <h2 className="mb-3 font-semibold">创建续费服务待办</h2>
        <Feedback action={action} />
        <Notice error={students.error || renewals.error} />
        <div className="grid gap-3">
          <Field label="搜索学生">
            <input
              className={fieldClass}
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </Field>
          <Field label="学生">
            <select
              className={fieldClass}
              value={student}
              onChange={(e) => setStudent(e.target.value)}
            >
              <option value="">选择学生</option>
              {students.data?.records.map((s) => (
                <option key={s.id} value={String(s.id)}>
                  {s.name}
                </option>
              ))}
            </select>
          </Field>
          <StaffPicker value={assignee} onChange={setAssignee} />
          <Field label="续费服务计划">
            <textarea
              className={fieldClass}
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </Field>
          <Field label="下次联系时间">
            <input
              type="datetime-local"
              className={fieldClass}
              value={nextAt}
              onChange={(e) => setNextAt(e.target.value)}
            />
          </Field>
          <button
            className={buttonClass}
            disabled={action.busy || !student || !assignee || !note.trim()}
            onClick={() =>
              void action.run(
                () =>
                  command('/engagement/renewals', {
                    branchId,
                    studentId: student,
                    assigneeId: assignee,
                    note,
                    nextAt: nextAt ? new Date(nextAt).toISOString() : null,
                    status: 'OPEN',
                  }),
                '续费待办已创建',
              )
            }
          >
            创建待办
          </button>
        </div>
      </Panel>
      {renewals.data?.map((r) => (
        <Panel key={r.id}>
          <p className="text-xs text-primary">
            {r.status === 'OPEN' ? '待联系' : r.status === 'CONTACTED' ? '已联系' : '已结束'} · 学生{' '}
            {students.data?.records.find((s) => String(s.id) === r.studentId)?.name ?? r.studentId}
          </p>
          <p className="my-2 whitespace-pre-wrap text-sm">{r.note}</p>
          {r.nextAt && (
            <p className="text-xs text-muted-fg">下次联系：{new Date(r.nextAt).toLocaleString()}</p>
          )}
          <RenewalUpdate key={r.id + r.note + r.status} branchId={branchId} renewal={r} />
        </Panel>
      ))}
    </>
  );
}

function ActivityCheckin({ activityId }: { activityId: string }) {
  const [open, setOpen] = useState(false);
  const signups = usePlatformList<
    { id: string; userId: string; status: string; createdAt: string }[]
  >(`/engagement/activities/${activityId}/signups`, open);
  const action = useAction();
  return (
    <div className="mt-3">
      <button className={secondaryClass} onClick={() => setOpen(!open)}>
        {open ? '收起报名名单' : '报名名单与签到'}
      </button>
      {open && (
        <>
          <Notice error={signups.error} />
          <Feedback action={action} />
          {signups.data?.length === 0 && <p className="mt-2 text-sm">暂无报名。</p>}
          {signups.data?.map((s) => (
            <div
              key={s.id}
              className="mt-2 flex items-center justify-between gap-2 border-t pt-2 text-sm"
            >
              <span>
                报名号 {s.id} ·{' '}
                {s.status === 'CHECKED_IN'
                  ? '已到店'
                  : s.status === 'CANCELLED'
                    ? '已取消'
                    : '已报名'}
              </span>
              {s.status === 'CONFIRMED' && (
                <button
                  disabled={action.busy}
                  className={secondaryClass}
                  onClick={() =>
                    void action.run(
                      () => command(`/engagement/signups/${s.id}/checkin`, {}),
                      '已确认到店',
                    )
                  }
                >
                  确认到店
                </button>
              )}
            </div>
          ))}
        </>
      )}
    </div>
  );
}
function RenewalUpdate({ branchId, renewal: r }: { branchId: string; renewal: Renewal }) {
  const [open, setOpen] = useState(false),
    [note, setNote] = useState(r.note),
    [status, setStatus] = useState(r.status),
    [owner, setOwner] = useState(r.assigneeId),
    [nextAt, setNextAt] = useState(
      r.nextAt
        ? new Date(new Date(r.nextAt).getTime() - new Date(r.nextAt).getTimezoneOffset() * 60000)
            .toISOString()
            .slice(0, 16)
        : '',
    );
  const action = useAction();
  return (
    <div className="mt-3">
      <button className={secondaryClass} onClick={() => setOpen(!open)}>
        更新跟进
      </button>
      {open && (
        <div className="mt-3 grid gap-3">
          <Feedback action={action} />
          <StaffPicker value={owner} onChange={setOwner} />
          <Field label="服务状态">
            <select
              className={fieldClass}
              value={status}
              onChange={(e) => setStatus(e.target.value as Renewal['status'])}
            >
              <option value="OPEN">待联系</option>
              <option value="CONTACTED">已联系</option>
              <option value="CLOSED">已结束</option>
            </select>
          </Field>
          <Field label="跟进结果">
            <textarea
              className={fieldClass}
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </Field>
          <Field label="下次联系">
            <input
              className={fieldClass}
              type="datetime-local"
              value={nextAt}
              onChange={(e) => setNextAt(e.target.value)}
            />
          </Field>
          <button
            className={buttonClass}
            disabled={action.busy || !owner || !note.trim()}
            onClick={() =>
              void action.run(
                () =>
                  command(
                    `/engagement/renewals/${r.id}`,
                    {
                      branchId,
                      studentId: r.studentId,
                      assigneeId: owner,
                      note,
                      status,
                      nextAt: nextAt ? new Date(nextAt).toISOString() : null,
                    },
                    'PUT',
                  ),
                '续费跟进已更新',
              )
            }
          >
            保存更新
          </button>
        </div>
      )}
    </div>
  );
}
function EngagementDashboard({ branchId }: { branchId: string }) {
  const dashboard = usePlatformList<{
    stages: { status: string; count: number }[];
    sources: { source: string; enquiries: number; enrollments: number }[];
    definition: string;
  }>(`/engagement/dashboard?branchId=${branchId}`);
  const referrals = usePlatformList<
    {
      enquiryId: string;
      referrerId: string;
      enrollmentId: string;
      status: string;
      fulfillmentNote: string;
    }[]
  >(`/engagement/referrals?branchId=${branchId}`);
  return (
    <div className="space-y-4">
      <Notice error={dashboard.error || referrals.error} />
      <Panel>
        <h2 className="font-semibold">招生转化</h2>
        <p className="my-2 text-xs text-muted-fg">{dashboard.data?.definition}</p>
        <div className="flex flex-wrap gap-4">
          {dashboard.data?.stages.map((s) => (
            <p key={s.status}>
              {leadLabels[s.status] ?? s.status} <strong>{s.count}</strong>
            </p>
          ))}
        </div>
        {dashboard.data?.sources.map((s) => (
          <p className="mt-3 text-sm" key={s.source}>
            {s.source || '自主咨询'} · 咨询 {s.enquiries} · 教务确认报名 {s.enrollments}
          </p>
        ))}
      </Panel>
      <Panel>
        <h2 className="font-semibold">推荐奖励记录</h2>
        <p className="my-2 text-xs text-muted-fg">
          以教务确认的有效报名为依据。这里只记录已实际兑现的奖励。
        </p>
        {referrals.data?.length === 0 && <p className="text-sm">暂无符合条件的推荐记录。</p>}
        {referrals.data?.map((r) => (
          <ReferralRow key={r.enquiryId} referral={r} />
        ))}
      </Panel>
    </div>
  );
}
function ReferralRow({
  referral: r,
}: {
  referral: {
    enquiryId: string;
    referrerId: string;
    enrollmentId: string;
    status: string;
    fulfillmentNote: string;
  };
}) {
  const [note, setNote] = useState('');
  const action = useAction();
  return (
    <div className="mt-3 border-t pt-3">
      <p className="text-sm">
        咨询 {r.enquiryId} · 推荐人 {r.referrerId} ·{' '}
        {r.status === 'FULFILLED' ? '已兑现' : '待兑现'}
      </p>
      {r.fulfillmentNote ? (
        <p className="my-2 text-sm">{r.fulfillmentNote}</p>
      ) : (
        <>
          <Field label="已兑现奖励的内容与凭据">
            <input className={fieldClass} value={note} onChange={(e) => setNote(e.target.value)} />
          </Field>
          <button
            className={`${secondaryClass} mt-2`}
            disabled={!note.trim() || action.busy}
            onClick={() =>
              void action.run(
                () => command(`/engagement/referrals/${r.enquiryId}/fulfill`, { note }),
                '已记录兑现结果',
              )
            }
          >
            记录已兑现奖励
          </button>
        </>
      )}
      <Feedback action={action} />
    </div>
  );
}
