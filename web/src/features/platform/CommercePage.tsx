import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { listCourses } from '@/features/course/api';
import { useAuthStore } from '@/features/auth/store';
import { command, platformRequest } from './client';
import { useAction, usePlatformList } from './hooks';
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
import {
  money,
  parsePrice,
  paymentLabels,
  fulfillmentLabels,
  refundLabels,
  type Product,
  type ProductInput,
  type Order,
  type Refund,
  type Aftersale,
  type Revenue,
} from '../../../../packages/contracts/business';
export function CommercePage() {
  const [branch, setBranch] = useState('');
  return (
    <div className="space-y-5">
      <div>
        <p className="text-xs tracking-[.2em] text-primary">STUDIO SHOP</p>
        <h1 className="mt-2 text-2xl font-semibold">商城与订单</h1>
        <p className="mt-2 text-sm text-muted-fg">
          课程、活动与画材在一个地方管理，支付与交付各有明确进度。
        </p>
      </div>
      <BranchPicker value={branch} onChange={setBranch} />
      {branch && <CommerceWorkspace key={branch} branchId={branch} />}
    </div>
  );
}
function CommerceWorkspace({ branchId }: { branchId: string }) {
  const [tab, setTab] = useState<'orders' | 'products' | 'operations'>('orders');
  const [editing, setEditing] = useState<Product | 'new' | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const products = usePlatformList<Product[]>(`/commerce/products?branchId=${branchId}`);
  const orders = usePlatformList<Order[]>(`/commerce/orders?branchId=${branchId}`);
  const revenue = usePlatformList<Revenue>(`/commerce/dashboard?branchId=${branchId}`);
  const [selectedStatus, setSelectedStatus] = useState('');
  return (
    <div className="space-y-4">
      <div className="grid gap-3 md:grid-cols-3">
        {[
          { label: '累计已支付', value: revenue.data?.paidMinor },
          { label: '已完成退款', value: revenue.data?.refundedMinor },
          { label: '净收款', value: revenue.data?.netReceiptsMinor },
        ].map((m) => (
          <Panel key={m.label}>
            <p className="text-xs text-muted-fg">{m.label}</p>
            <strong className="mt-2 block text-2xl">
              {m.value === undefined ? '—' : `¥ ${money(m.value)}`}
            </strong>
          </Panel>
        ))}
      </div>
      <p className="text-xs text-muted-fg">{revenue.data?.definition}</p>
      <Notice error={products.error || orders.error || revenue.error} />
      <div className="flex flex-wrap gap-2">
        <button
          className={tab === 'orders' ? buttonClass : secondaryClass}
          onClick={() => setTab('orders')}
        >
          订单与履约
        </button>
        <button
          className={tab === 'products' ? buttonClass : secondaryClass}
          onClick={() => setTab('products')}
        >
          商品管理
        </button>
        <button
          className={tab === 'operations' ? buttonClass : secondaryClass}
          onClick={() => setTab('operations')}
        >
          退款待办与对账
        </button>
      </div>
      {tab === 'operations' ? (
        <>
          <CommerceOperations branchId={branchId} onSelect={setSelected} />
          {selected && (
            <OrderOperations key={selected} orderId={selected} onClose={() => setSelected(null)} />
          )}
        </>
      ) : tab === 'products' ? (
        <>
          <button className={buttonClass} onClick={() => setEditing('new')}>
            新建商品
          </button>
          {editing && (
            <ProductEditor
              key={typeof editing === 'string' ? editing : editing.id}
              branchId={branchId}
              product={editing === 'new' ? undefined : editing}
              onSaved={() => setEditing(null)}
            />
          )}
          <div className="grid gap-3 lg:grid-cols-2">
            {products.data?.map((p) => (
              <Panel key={p.id}>
                <p className="text-xs text-primary">
                  {{ COURSE: '课程服务', ACTIVITY: '活动门票', PHYSICAL: '实物商品' }[p.type]} ·{' '}
                  {p.published ? '已上架' : '未上架'}
                </p>
                <h2 className="my-2 text-lg font-semibold">{p.title}</h2>
                <p className="mb-3 whitespace-pre-wrap text-sm">{p.description}</p>
                <div className="flex justify-between">
                  <strong>
                    ¥ {money(p.priceMinor)} · 库存 {p.stock}
                  </strong>
                  <button className={secondaryClass} onClick={() => setEditing(p)}>
                    编辑 / 上下架
                  </button>
                </div>
              </Panel>
            ))}
          </div>
        </>
      ) : (
        <>
          <select
            aria-label="筛选订单状态"
            className={`${fieldClass} max-w-xs`}
            value={selectedStatus}
            onChange={(e) => setSelectedStatus(e.target.value)}
          >
            <option value="">所有支付状态</option>
            {Object.entries(paymentLabels).map(([value, label]) => (
              <option value={value} key={value}>
                {label}
              </option>
            ))}
          </select>
          {selected && (
            <OrderOperations key={selected} orderId={selected} onClose={() => setSelected(null)} />
          )}
          <div className="grid gap-3 lg:grid-cols-2">
            {orders.data
              ?.filter((o) => !selectedStatus || o.paymentStatus === selectedStatus)
              .map((o) => (
                <Panel key={o.id}>
                  <div className="flex justify-between">
                    <h2 className="font-semibold">
                      {o.productTitle} × {o.quantity}
                    </h2>
                    <strong>¥ {money(o.totalMinor)}</strong>
                  </div>
                  <p className="mt-2 text-xs text-muted-fg">
                    {new Date(o.createdAt).toLocaleString()} · 订单 {o.id}
                  </p>
                  <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
                    <p className="text-sm">
                      {paymentLabels[o.paymentStatus]} · {fulfillmentLabels[o.fulfillmentStatus]}
                    </p>
                    <button className={secondaryClass} onClick={() => setSelected(o.id)}>
                      查看 / 处理
                    </button>
                  </div>
                </Panel>
              ))}
          </div>
          {orders.data?.length === 0 && (
            <Panel>
              <p className="text-sm">尚无订单。</p>
            </Panel>
          )}
        </>
      )}
    </div>
  );
}
export function ProductEditor({
  branchId,
  product,
  onSaved,
}: {
  branchId: string;
  product?: Product;
  onSaved: () => void;
}) {
  const [draft, setDraft] = useState<ProductInput>(
    product ?? {
      branchId,
      version: 0,
      type: 'PHYSICAL',
      title: '',
      description: '',
      priceMinor: 0,
      stock: 10,
      lessonUnits: 0,
      published: false,
    },
  );
  const [price, setPrice] = useState(product ? money(product.priceMinor) : '');
  const user = useAuthStore((s) => s.user);
  const courses = useQuery({
    queryKey: ['platform', user?.id, 'commerce-courses'],
    queryFn: () => listCourses(1, 100),
    enabled: draft.type === 'COURSE',
  });
  const action = useAction();
  return (
    <Panel>
      <form
        className="grid gap-3"
        onSubmit={(e) => {
          e.preventDefault();
          void action.run(async () => {
            await command('/commerce/products', { ...draft, priceMinor: parsePrice(price) });
            onSaved();
          });
        }}
      >
        <Field label="商品名称">
          <input
            required
            className={fieldClass}
            value={draft.title}
            onChange={(e) => setDraft({ ...draft, title: e.target.value })}
          />
        </Field>
        <Field label="商品类型">
          <select
            disabled={Boolean(product)}
            className={fieldClass}
            value={draft.type}
            onChange={(e) =>
              setDraft({
                ...draft,
                type: e.target.value as Product['type'],
                lessonUnits: e.target.value === 'COURSE' ? 1 : 0,
              })
            }
          >
            <option value="PHYSICAL">实物商品</option>
            <option value="COURSE">课程服务</option>
            <option value="ACTIVITY">收费活动</option>
          </select>
        </Field>
        <Field label="商品介绍">
          <textarea
            required
            className={fieldClass}
            value={draft.description}
            onChange={(e) => setDraft({ ...draft, description: e.target.value })}
          />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="售价（元）">
            <input
              required
              inputMode="decimal"
              className={fieldClass}
              value={price}
              onChange={(e) => setPrice(e.target.value)}
            />
          </Field>
          <Field label="剩余可售数量">
            <input
              type="number"
              min="0"
              className={fieldClass}
              value={draft.stock}
              onChange={(e) => setDraft({ ...draft, stock: Number(e.target.value) })}
            />
          </Field>
        </div>
        {draft.type === 'COURSE' && (
          <>
            <Notice error={courses.error} />
            <Field label="适用课程">
              <select
                required
                className={fieldClass}
                value={draft.courseId ?? ''}
                onChange={(e) => setDraft({ ...draft, courseId: e.target.value })}
              >
                <option value="">选择课程</option>
                {courses.data?.items.map((c) => (
                  <option key={c.id} value={String(c.id)}>
                    {c.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="每份商品包含课时">
              <input
                type="number"
                min="1"
                max="10000"
                className={fieldClass}
                value={draft.lessonUnits}
                onChange={(e) => setDraft({ ...draft, lessonUnits: Number(e.target.value) })}
              />
            </Field>
            <p className="text-xs text-muted-fg">购买课程后发放课时权益，具体班级仍需教务安排。</p>
          </>
        )}
        <label className="text-sm">
          <input
            type="checkbox"
            checked={draft.published}
            onChange={(e) => setDraft({ ...draft, published: e.target.checked })}
          />{' '}
          在小程序上架
        </label>
        <Feedback action={action} />
        <div className="flex gap-2">
          <button disabled={action.busy} className={buttonClass}>
            保存商品
          </button>
          <button className={secondaryClass} type="button" onClick={onSaved}>
            关闭
          </button>
          {product && (
            <button
              type="button"
              className={secondaryClass}
              disabled={action.busy}
              onClick={() =>
                void action.run(async () => {
                  const latest = (
                    await platformRequest<Product[]>(`/commerce/products?branchId=${branchId}`)
                  ).find((item) => item.id === product.id);
                  if (!latest) throw new Error('商品已不存在，请关闭后刷新列表');
                  setDraft(latest);
                  setPrice(money(latest.priceMinor));
                }, '已重新载入商品和最新库存')
              }
            >
              重新载入商品（替换当前输入）
            </button>
          )}
        </div>
      </form>
    </Panel>
  );
}
function OrderOperations({ orderId, onClose }: { orderId: string; onClose: () => void }) {
  const order = usePlatformList<Order>(`/commerce/orders/${orderId}`);
  const refunds = usePlatformList<Refund[]>(`/commerce/orders/${orderId}/refunds`);
  const aftersales = usePlatformList<Aftersale[]>(`/commerce/orders/${orderId}/aftersales`);
  const [reason, setReason] = useState('');
  const [tracking, setTracking] = useState('');
  const [response, setResponse] = useState('');
  const action = useAction();
  const o = order.data;
  return (
    <Panel>
      <div className="flex justify-between">
        <h2 className="font-semibold">订单处理</h2>
        <button
          className={secondaryClass}
          disabled={action.busy}
          onClick={() =>
            void action.run(
              () => command(`/commerce/orders/${orderId}/reconcile`, {}),
              '已向微信核实，请以最新订单状态为准',
            )
          }
        >
          主动对账
        </button>
        <button className={secondaryClass} onClick={onClose}>
          关闭详情
        </button>
      </div>
      <Notice error={order.error || refunds.error || aftersales.error} />
      <Feedback action={action} />
      {o && (
        <div className="mt-4 space-y-3">
          <p>
            {o.productTitle} · ¥ {money(o.totalMinor)}
          </p>
          <p className="text-sm">
            {paymentLabels[o.paymentStatus]} · {fulfillmentLabels[o.fulfillmentStatus]}
          </p>
          {o.paymentStatus === 'UNPAID' && (
            <>
              <p className="text-sm text-muted-fg">请购买人在小程序订单中支付。</p>
              <button
                className={secondaryClass}
                disabled={action.busy}
                onClick={() =>
                  void action.run(
                    () => command(`/commerce/orders/${o.id}/cancel`, {}),
                    '订单已取消',
                  )
                }
              >
                取消未支付订单
              </button>
            </>
          )}
          {o.paymentStatus === 'PAID' &&
            o.productType !== 'COURSE' &&
            ['READY', 'SHIPPED'].includes(o.fulfillmentStatus) && (
              <div className="grid gap-2">
                {o.productType === 'PHYSICAL' && o.fulfillmentMethod === 'DELIVERY' && (
                  <Field label="物流单号">
                    <input
                      className={fieldClass}
                      value={tracking}
                      onChange={(e) => setTracking(e.target.value)}
                    />
                  </Field>
                )}
                <div className="flex gap-2">
                  {o.productType === 'PHYSICAL' &&
                    o.fulfillmentStatus === 'READY' &&
                    o.fulfillmentMethod === 'DELIVERY' && (
                      <button
                        className={secondaryClass}
                        disabled={action.busy || !tracking.trim()}
                        onClick={() =>
                          void action.run(
                            () =>
                              command(`/commerce/orders/${o.id}/fulfillment`, {
                                status: 'SHIPPED',
                                trackingNumber: tracking,
                              }),
                            '已记录发货',
                          )
                        }
                      >
                        确认发货
                      </button>
                    )}
                  <button
                    className={buttonClass}
                    disabled={
                      action.busy ||
                      (o.productType === 'PHYSICAL' &&
                        o.fulfillmentMethod === 'DELIVERY' &&
                        o.fulfillmentStatus !== 'SHIPPED')
                    }
                    onClick={() =>
                      void action.run(
                        () =>
                          command(`/commerce/orders/${o.id}/fulfillment`, {
                            status: o.productType === 'ACTIVITY' ? 'REDEEMED' : 'COMPLETED',
                          }),
                        '交付状态已更新',
                      )
                    }
                  >
                    {o.productType === 'ACTIVITY'
                      ? '核销活动门票'
                      : o.fulfillmentStatus === 'SHIPPED'
                        ? '确认配送完成'
                        : '确认已自提'}
                  </button>
                </div>
              </div>
            )}
          {o.productType === 'PHYSICAL' && (
            <div className="rounded-xl bg-white/60 p-3 text-sm">
              <p>交付方式：{o.fulfillmentMethod === 'DELIVERY' ? '配送到家' : '画室自提'}</p>
              {o.fulfillmentMethod === 'DELIVERY' && (
                <>
                  <p>
                    {o.recipientName} · {o.recipientPhone}
                  </p>
                  <p>{o.address}</p>
                </>
              )}
            </div>
          )}
          {o.trackingNumber && <p className="text-sm">物流单号：{o.trackingNumber}</p>}
          <Field label="退款或售后原因">
            <textarea
              className={fieldClass}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          </Field>
          <div className="flex gap-2">
            {o.paymentStatus === 'PAID' && (
              <button
                className={secondaryClass}
                disabled={action.busy || !reason.trim()}
                onClick={() =>
                  void action.run(
                    () =>
                      command(`/commerce/orders/${o.id}/refunds`, {
                        reason,
                        idempotencyKey: `refund-${o.id}`,
                      }),
                    '退款申请已提交，请查看退款进度',
                  )
                }
              >
                申请整单退款
              </button>
            )}
            <button
              className={secondaryClass}
              disabled={action.busy || !reason.trim()}
              onClick={() =>
                void action.run(
                  () => command(`/commerce/orders/${o.id}/aftersales`, { reason }),
                  '售后已登记',
                )
              }
            >
              登记售后
            </button>
          </div>
          {refunds.data?.map((r) => (
            <p key={r.id} className="rounded-xl bg-white p-3 text-sm">
              退款 ¥ {money(r.amountMinor)} · {refundLabels[r.status] ?? r.status}
              <span className="block text-muted-fg">{r.reason}</span>
            </p>
          ))}
          {aftersales.data?.length ? (
            <>
              <Field label="售后处理说明">
                <textarea
                  className={fieldClass}
                  value={response}
                  onChange={(e) => setResponse(e.target.value)}
                />
              </Field>
              {aftersales.data.map((a) => (
                <div key={a.id} className="rounded-xl bg-white p-3 text-sm">
                  <p>{a.reason}</p>
                  <p className="my-2 text-muted-fg">
                    {a.response || '待回复'} ·{' '}
                    {a.status === 'CLOSED'
                      ? '已关闭'
                      : a.status === 'PROCESSING'
                        ? '处理中'
                        : '待处理'}
                  </p>
                  {a.status !== 'CLOSED' && (
                    <div className="flex gap-2">
                      {['PROCESSING', 'CLOSED'].map((status) => (
                        <button
                          key={status}
                          className={secondaryClass}
                          disabled={action.busy || !response.trim()}
                          onClick={() =>
                            void action.run(
                              () =>
                                command(`/commerce/orders/${o.id}/aftersales/${a.id}/resolve`, {
                                  status,
                                  response,
                                }),
                              '售后处理已记录',
                            )
                          }
                        >
                          {status === 'CLOSED' ? '回复并关闭' : '回复并继续处理'}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </>
          ) : null}
        </div>
      )}
    </Panel>
  );
}

function CommerceOperations({
  branchId,
  onSelect,
}: {
  branchId: string;
  onSelect: (id: string) => void;
}) {
  const pending = usePlatformList<
    (Refund & { lastError: string | null; attempts: number; updatedAt: string })[]
  >(`/commerce/refunds/pending?branchId=${branchId}`);
  const records = usePlatformList<
    { id: string; orderId: string; providerStatus: string; result: string; checkedAt: string }[]
  >(`/commerce/reconciliations?branchId=${branchId}`);
  const action = useAction();
  return (
    <div className="space-y-4">
      <Feedback action={action} />
      <Notice error={pending.error || records.error} />
      <Panel>
        <h2 className="font-semibold">待处理退款</h2>
        <p className="my-2 text-xs text-muted-fg">
          重试会核实原退款结果。微信异常退款请先在商户平台处理，再重新核实。
        </p>
        {pending.data?.length === 0 && <p>当前没有退款积压。</p>}
        {pending.data?.map((r) => (
          <div key={r.id} className="mt-3 rounded-xl bg-white p-3">
            <p>
              订单 {r.orderId} · ¥ {money(r.amountMinor)} · {refundLabels[r.status] ?? r.status}
            </p>
            <p className="my-2 text-sm">
              {r.reason} · 已尝试 {r.attempts} 次
            </p>
            {r.lastError && <p className="text-sm text-red-600">{r.lastError}</p>}
            <div className="mt-2 flex gap-2">
              <button className={secondaryClass} onClick={() => onSelect(r.orderId)}>
                查看订单
              </button>
              <button
                disabled={action.busy}
                className={buttonClass}
                onClick={() =>
                  void action.run(
                    () => command(`/commerce/orders/${r.orderId}/refunds/${r.id}/retry`, {}),
                    '原退款已重新核实，请查看最新状态',
                  )
                }
              >
                核实并重试原退款
              </button>
            </div>
          </div>
        ))}
      </Panel>
      <Panel>
        <h2 className="font-semibold">对账记录</h2>
        <p className="my-2 text-xs text-muted-fg">
          在订单详情中可发起主动对账，以下保留服务端向微信核实的结果。
        </p>
        {records.data?.map((r) => (
          <div className="mt-2 border-t pt-2 text-sm" key={r.id}>
            <button className="text-primary" onClick={() => onSelect(r.orderId)}>
              订单 {r.orderId}
            </button>
            <p>
              {r.providerStatus} · {r.result} · {new Date(r.checkedAt).toLocaleString()}
            </p>
          </div>
        ))}
      </Panel>
    </div>
  );
}
