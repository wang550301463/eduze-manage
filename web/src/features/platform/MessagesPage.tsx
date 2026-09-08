import type { NotificationMessage as Message } from '../../../../packages/contracts';
import { usePlatformList, useAction } from './hooks';
import { Link } from 'react-router-dom';
import { command } from './client';
import { Panel, buttonClass, secondaryClass, Feedback, Notice } from './ui';

export function MessagesPage() {
  const messages = usePlatformList<Message[]>('/notifications');
  const action = useAction();
  return (
    <div className="space-y-5">
      <h1 className="text-2xl font-semibold">消息与待办</h1>
      <p className="text-sm text-muted-fg">发送结果、阅读和业务确认分别记录。</p>
      <Feedback action={action} />
      <Notice error={messages.error} />
      {messages.data?.map((m) => (
        <Panel key={m.id}>
          <div className="flex justify-between">
            <h2 className="font-semibold">{m.title}</h2>
            <span className="text-xs text-muted-fg">{new Date(m.createdAt).toLocaleString()}</span>
          </div>
          <p className="my-3 whitespace-pre-wrap text-sm">{m.body}</p>
          <p className="mb-3 text-xs text-muted-fg">
            {m.readAt ? '已读' : '未读'} · {m.confirmedAt ? '已确认' : '待确认'}
          </p>
          <div className="flex gap-2">
            {m.path?.startsWith('/') && !m.path.startsWith('//') && (
              <Link to={m.path} className={secondaryClass}>
                查看业务
              </Link>
            )}
            {!m.readAt && (
              <button
                className={secondaryClass}
                disabled={action.busy}
                onClick={() =>
                  void action.run(() => command(`/notifications/${m.id}/read`, {}), '已标记阅读')
                }
              >
                标记已读
              </button>
            )}
            {!m.confirmedAt && (
              <button
                className={buttonClass}
                disabled={action.busy}
                onClick={() =>
                  void action.run(() => command(`/notifications/${m.id}/confirm`, {}), '已确认')
                }
              >
                确认收到
              </button>
            )}
          </div>
        </Panel>
      ))}
      {messages.data?.length === 0 && (
        <Panel>
          <p>当前没有消息。</p>
        </Panel>
      )}
    </div>
  );
}
