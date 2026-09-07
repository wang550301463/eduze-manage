import { useState } from 'react';
import type { ColumnDef } from '@tanstack/react-table';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Checkbox } from '@/components/ui/Checkbox';
import { DataTable } from '@/components/ui/DataTable';
import { DatePicker } from '@/components/ui/DatePicker';
import { DialogFrame } from '@/components/ui/Dialog';
import { DrawerFrame } from '@/components/ui/Drawer';
import { DropdownMenuFrame } from '@/components/ui/DropdownMenu';
import { EmptyState } from '@/components/ui/EmptyState';
import { Input } from '@/components/ui/Input';
import { KPICard } from '@/components/ui/KPICard';
import { Label } from '@/components/ui/Label';
import { RadioGroup, RadioGroupItem } from '@/components/ui/RadioGroup';
import { SimpleSelect } from '@/components/ui/Select';
import { SheetFrame } from '@/components/ui/Sheet';
import { Skeleton } from '@/components/ui/Skeleton';
import { Switch } from '@/components/ui/Switch';
import { Textarea } from '@/components/ui/Textarea';
import { SimpleTooltip, TooltipProvider } from '@/components/ui/Tooltip';
import { toast } from '@/lib/toast';

type DemoRow = { id: string; name: string; branch: string };

const columns: ColumnDef<DemoRow>[] = [
  { accessorKey: 'name', header: '姓名' },
  { accessorKey: 'branch', header: '校区' },
];

const mockData: DemoRow[] = [
  { id: '1', name: '张小明', branch: '朝阳校区' },
  { id: '2', name: '李朵朵', branch: '海淀校区' },
  { id: '3', name: '王乐乐', branch: '朝阳校区' },
  { id: '4', name: '赵星星', branch: '西城校区' },
  { id: '5', name: '陈果果', branch: '海淀校区' },
];

export function HomePage(): JSX.Element {
  const [page, setPage] = useState(1);
  const [checked, setChecked] = useState(false);
  const [switchOn, setSwitchOn] = useState(true);
  const [selectVal, setSelectVal] = useState('a');

  return (
    <TooltipProvider>
      <div className="min-h-screen bg-background">
        <header className="sticky top-0 z-20 border-b border-border bg-background/95 px-6 py-4 backdrop-blur">
          <div className="mx-auto flex max-w-6xl items-center justify-between">
            <h1 className="font-serif text-xl font-bold text-primary">EduZE Manage</h1>
            <nav className="flex items-center gap-4 text-sm">
              <span className="text-muted-fg">设计系统 Demo</span>
              <Link to="/login" className="text-primary hover:underline">
                登录
              </Link>
            </nav>
          </div>
        </header>

        <main className="mx-auto max-w-6xl space-y-10 px-6 py-8">
          <section>
            <h2 className="mb-4 font-serif text-lg font-semibold">KPI 卡片</h2>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <KPICard title="在读学员" value={1280} trend={{ value: 12, direction: 'up' }} />
              <KPICard title="本周出勤" value={94} suffix="%" />
              <KPICard title="待跟进" value={23} />
              <KPICard title="本月续费" value={86} suffix="%" trend={{ value: 3, direction: 'down' }} />
            </div>
          </section>

          <section>
            <h2 className="mb-4 font-serif text-lg font-semibold">表单元素</h2>
            <div className="flex flex-wrap gap-3">
              <Button>主按钮</Button>
              <Button variant="secondary">次要</Button>
              <Button variant="ghost">幽灵</Button>
              <Button variant="danger">危险</Button>
              <Button variant="link">链接</Button>
              <Button size="sm">小</Button>
            </div>
            <div className="mt-4 grid max-w-md gap-3">
              <div>
                <Label htmlFor="demo-input">姓名</Label>
                <Input id="demo-input" placeholder="请输入姓名" className="mt-1" />
              </div>
              <Textarea placeholder="备注" rows={2} />
              <SimpleSelect
                options={[
                  { value: 'a', label: '朝阳校区' },
                  { value: 'b', label: '海淀校区' },
                ]}
                value={selectVal}
                onValueChange={setSelectVal}
                aria-label="校区"
              />
              <div className="flex items-center gap-2">
                <Checkbox
                  id="demo-check"
                  checked={checked}
                  onCheckedChange={(v) => setChecked(v === true)}
                  aria-label="同意协议"
                />
                <Label htmlFor="demo-check">同意协议</Label>
              </div>
              <RadioGroup defaultValue="1" className="flex gap-4">
                <div className="flex items-center gap-2">
                  <RadioGroupItem value="1" id="r1" />
                  <Label htmlFor="r1">选项 A</Label>
                </div>
                <div className="flex items-center gap-2">
                  <RadioGroupItem value="2" id="r2" />
                  <Label htmlFor="r2">选项 B</Label>
                </div>
              </RadioGroup>
              <div className="flex items-center gap-2">
                <Switch
                  id="demo-switch"
                  checked={switchOn}
                  onCheckedChange={setSwitchOn}
                  aria-label="启用通知"
                />
                <Label htmlFor="demo-switch">启用通知</Label>
              </div>
              <DatePicker />
            </div>
          </section>

          <section>
            <h2 className="mb-4 font-serif text-lg font-semibold">弹层 & Toast</h2>
            <div className="flex flex-wrap gap-2">
              <DialogFrame
                trigger={<Button variant="secondary">Dialog</Button>}
                title="确认操作"
                description="这是一个对话框示例"
                footer={<Button>确定</Button>}
              >
                <p className="text-sm text-muted-fg">内容区域</p>
              </DialogFrame>
              <SheetFrame
                trigger={<Button variant="secondary">Sheet</Button>}
                title="学员详情"
                footer={<Button className="w-full">保存</Button>}
              >
                <p className="text-sm">右侧抽屉内容</p>
              </SheetFrame>
              <DrawerFrame
                trigger={<Button variant="secondary">Drawer</Button>}
                title="筛选"
              >
                <p className="text-sm">底部抽屉（移动端筛选）</p>
              </DrawerFrame>
              <DropdownMenuFrame
                trigger={<Button variant="ghost">菜单</Button>}
                items={[
                  { label: '编辑', onSelect: () => undefined },
                  { label: '删除', onSelect: () => undefined },
                ]}
              />
              <SimpleTooltip content="提示信息">
                <Button variant="ghost">Tooltip</Button>
              </SimpleTooltip>
              <Button onClick={() => toast.success('已保存')}>Success Toast</Button>
              <Button
                variant="secondary"
                onClick={() =>
                  toast.undo({ message: '已删除', onUndo: () => toast.info('已撤销') })
                }
              >
                Undo Toast
              </Button>
            </div>
          </section>

          <section>
            <h2 className="mb-4 font-serif text-lg font-semibold">数据表格</h2>
            <DataTable
              columns={columns}
              data={mockData}
              rowKey={(r) => r.id}
              pageState={{
                page,
                pageSize: 10,
                total: mockData.length,
                onChange: ({ page: p }) => setPage(p),
              }}
              mobileCardRender={(row) => (
                <>
                  <p className="font-medium">{row.name}</p>
                  <p className="text-sm text-muted-fg">{row.branch}</p>
                </>
              )}
              onRowClick={() => toast.info('点击了行')}
            />
          </section>

          <section>
            <h2 className="mb-4 font-serif text-lg font-semibold">状态展示</h2>
            <Skeleton className="mb-4 h-8 w-48" />
            <EmptyState
              title="暂无学员"
              description="点击上方按钮添加第一位学员"
              action={{ label: '添加学员', onClick: () => toast.success('跳转添加') }}
            />
          </section>
        </main>
      </div>
    </TooltipProvider>
  );
}
