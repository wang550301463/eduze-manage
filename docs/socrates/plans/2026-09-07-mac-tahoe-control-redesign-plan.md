# Tahoe Control Visual Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use socrates:subagent-driven-development (recommended) or socrates:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle the entire EduZE Manage admin UI to macOS Tahoe Control chrome while keeping the existing warm terracotta primary, without changing business logic or APIs.

**Architecture:** Token-first restyle. Update CSS variables and Tailwind theme, then shell (Sidebar/Header/MobileNav), then shared `components/ui`, then business pages by adjusting layout classes only. Preserve snowflake-ID string handling, `navItemEnd`, Dialog unmount-on-close, and DataTable single empty state.

**Tech Stack:** React + Vite, Tailwind CSS, CVA, existing Radix-based UI wrappers, React Router, TanStack Query.

**Spec:** `docs/socrates/specs/2026-09-07-mac-tahoe-control-redesign-design.md`

---

## File map

| Area | Files | Responsibility |
|------|-------|----------------|
| Tokens | `web/src/styles/globals.css`, `web/tailwind.config.ts`, optionally `web/src/styles/fonts.css` | Surfaces, radii, type stack |
| Shell | `web/src/app/shell/AppLayout.tsx`, `Sidebar.tsx`, `Header.tsx`, `MobileNav.tsx` | Settings-style chrome |
| UI kit | `web/src/components/ui/Button.tsx`, `Input.tsx`, `Select.tsx`, `Dialog.tsx`, `Sheet.tsx`, `Drawer.tsx`, `DataTable.tsx`, `EmptyState.tsx`, `KPICard.tsx`, `Pagination.tsx`, `Toast.tsx` | Control language |
| Auth | `web/src/features/auth/pages/LoginPage.tsx` | First impression |
| High-freq pages | dashboard, student, attendance, leave, schedule, teacher availability / workbench | Layout class pass |
| Standard pages | course / class-group / classroom / settings | Layout class pass |
| Tests | existing `web/src/components/ui/__tests__/*`, `web/src/app/shell/nav-config.test.ts` | Guard regressions |

---

### Task 1: Design tokens (Tahoe surface + keep warm primary)

**Files:**
- Modify: `web/src/styles/globals.css`
- Modify: `web/tailwind.config.ts`
- Modify: `web/src/styles/fonts.css` (font-family aliases only if needed)

- [ ] **Step 1: Update CSS variables for Tahoe Control surfaces**

In `web/src/styles/globals.css`, keep warm `--primary` near current `12 82% 42%`. Retarget neutrals:

```css
:root {
  /* keep warm brand */
  --primary: 12 82% 42%;
  --primary-fg: 0 0% 100%;
  --secondary: 12 40% 94%;
  /* Tahoe Control neutrals (not Apple blue) */
  --background: 240 5% 96%; /* ~#F2F2F7 */
  --foreground: 240 6% 10%;
  --muted: 240 5% 92%;
  --muted-fg: 240 4% 46%;
  --border: 240 6% 90%;
  --card: 0 0% 100%;
  --card-fg: 240 6% 10%;
}
```

If `--card` is new, also wire it in `tailwind.config.ts` under `colors.card` / `card-fg`.

- [ ] **Step 2: Prefer system type stack for Mac feel**

In `web/tailwind.config.ts`:

```ts
fontFamily: {
  sans: [
    '-apple-system',
    'BlinkMacSystemFont',
    '"SF Pro Text"',
    '"PingFang SC"',
    '"IBM Plex Sans"',
    'system-ui',
    'sans-serif',
  ],
  // keep serif for any remaining display headings that intentionally use font-serif
  serif: ['"Noto Serif"', 'Georgia', 'serif'],
  mono: ['"IBM Plex Mono"', 'ui-monospace', 'monospace'],
},
borderRadius: {
  sm: '8px',
  md: '10px',
  lg: '14px',
  xl: '18px',
},
```

- [ ] **Step 3: Smoke-check tokens build**

Run: `cd web && npm run build`  
Expected: success (no TS/CSS errors).

- [ ] **Step 4: Commit**

```bash
git add web/src/styles/globals.css web/tailwind.config.ts
git commit -m "style: retarget tokens for Tahoe Control surfaces"
```

---

### Task 2: Button + Input + Select control language

**Files:**
- Modify: `web/src/components/ui/Button.tsx`
- Modify: `web/src/components/ui/Input.tsx`
- Modify: `web/src/components/ui/Select.tsx`
- Test: `web/src/components/ui/__tests__/Button.test.tsx`, `Select.test.tsx`

- [ ] **Step 1: Soften Button radii / secondary to Settings-like gray**

In `Button.tsx` `buttonVariants` base + variants:

```ts
export const buttonVariants = cva(
  'inline-flex items-center justify-center rounded-[10px] font-medium transition-colors disabled:opacity-50 disabled:pointer-events-none',
  {
    variants: {
      variant: {
        default: 'bg-primary text-primary-fg hover:bg-primary/90',
        secondary: 'bg-muted text-foreground hover:bg-muted/80',
        ghost: 'bg-transparent hover:bg-muted',
        danger: 'bg-error text-primary-fg hover:bg-error/90',
        link: 'underline text-primary hover:text-primary/80',
      },
      size: {
        sm: 'h-8 px-3 text-sm rounded-lg',
        md: 'h-9 px-4 text-sm',
        lg: 'h-11 px-5 text-base',
      },
    },
    defaultVariants: { variant: 'default', size: 'md' },
  },
);
```

Do **not** change primary to blue.

- [ ] **Step 2: Input / Select fill style**

`Input.tsx` target classes roughly:

```tsx
className={cn(
  'flex h-9 w-full rounded-[10px] border border-border bg-muted/60 px-3 text-sm',
  'placeholder:text-muted-fg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30',
  className,
)}
```

Apply the same radius/fill language to `SimpleSelect` trigger in `Select.tsx`. Keep existing `value={value || undefined}` empty-value fix.

- [ ] **Step 3: Run UI unit tests**

Run: `cd web && npm test -- --run src/components/ui/__tests__/Button.test.tsx src/components/ui/__tests__/Select.test.tsx`  
Expected: PASS (update class assertions only if tests pin exact class strings).

- [ ] **Step 4: Commit**

```bash
git add web/src/components/ui/Button.tsx web/src/components/ui/Input.tsx web/src/components/ui/Select.tsx web/src/components/ui/__tests__
git commit -m "style: Tahoe-ify Button Input Select controls"
```

---

### Task 3: Shell — Sidebar / Header / MobileNav / AppLayout

**Files:**
- Modify: `web/src/app/shell/Sidebar.tsx`
- Modify: `web/src/app/shell/Header.tsx`
- Modify: `web/src/app/shell/MobileNav.tsx`
- Modify: `web/src/app/shell/AppLayout.tsx`
- Test: `web/src/app/shell/nav-config.test.ts`

- [ ] **Step 1: Sidebar Settings look (warm solid active)**

Replace translucent primary tint active with solid warm capsule:

```tsx
className={({ isActive }) =>
  cn(
    'flex items-center gap-3 rounded-[10px] px-3 py-2 text-sm transition-colors',
    isActive
      ? 'bg-primary font-medium text-primary-fg'
      : 'text-foreground/80 hover:bg-muted',
  )
}
```

Sidebar container:

```tsx
'hidden h-full shrink-0 flex-col border-r border-border/80 bg-[#F2F2F7] lg:flex',
collapsed ? 'w-16' : 'w-[210px]',
```

Keep `end={navItemEnd(item.path)}`.

- [ ] **Step 2: AppLayout main canvas**

```tsx
<div className="flex min-h-screen bg-background">
  ...
  <main id="main" className="flex-1 overflow-y-auto p-4 md:p-6">
    <Outlet />
  </main>
</div>
```

Header: light bar `bg-background/80 backdrop-blur` optional; avoid heavy shadow. Prefer `border-b border-border`.

- [ ] **Step 3: Mirror active styles in MobileNav**

Same active capsule rules as Sidebar; keep `navItemEnd`.

- [ ] **Step 4: Run nav tests**

Run: `cd web && npm test -- --run src/app/shell/nav-config.test.ts`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add web/src/app/shell
git commit -m "style: Tahoe Control shell sidebar and header"
```

---

### Task 4: Dialog / Sheet / DataTable / EmptyState / KPICard

**Files:**
- Modify: `web/src/components/ui/Dialog.tsx`
- Modify: `web/src/components/ui/Sheet.tsx` and/or `Drawer.tsx`
- Modify: `web/src/components/ui/DataTable.tsx`
- Modify: `web/src/components/ui/EmptyState.tsx`
- Modify: `web/src/components/ui/KPICard.tsx`
- Test: `web/src/components/ui/__tests__/Dialog.test.tsx`, `DataTable.test.tsx`, `EmptyState.test.tsx`, `KPICard.test.tsx`

- [ ] **Step 1: Dialog chrome**

Content panel: `rounded-xl`, light shadow, footer with cancel left / primary right (already common — enforce in DialogFooter spacing `gap-2 justify-end` with cancel ordered first in callers only when editing those callers). Preserve controlled `open===false` unmount behavior in `DialogFrame`.

- [ ] **Step 2: DataTable / EmptyState**

Keep single empty-state render. Style empty as short copy + optional CTA wrapper classes (`py-12 text-center text-muted-fg`). KPI cards: white `bg-card` (or `bg-white`) on gray canvas, `rounded-xl`, minimal shadow `shadow-sm`.

- [ ] **Step 3: Run related tests**

Run: `cd web && npm test -- --run src/components/ui/__tests__/Dialog.test.tsx src/components/ui/__tests__/DataTable.test.tsx src/components/ui/__tests__/EmptyState.test.tsx src/components/ui/__tests__/KPICard.test.tsx`  
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add web/src/components/ui
git commit -m "style: Tahoe-ify dialogs tables and KPI cards"
```

---

### Task 5: Login page

**Files:**
- Modify: `web/src/features/auth/pages/LoginPage.tsx`

- [ ] **Step 1: Restyle login to Settings-like card on gray canvas**

Use `bg-background` full page, centered white card `rounded-xl p-8 shadow-sm border border-border`, warm primary submit button. No blue CTAs. Keep existing auth submit handlers unchanged.

- [ ] **Step 2: Manual check**

Open login locally or via deployed static after later deploy. Expected: warm primary button, gray canvas, no functional login break.

- [ ] **Step 3: Commit**

```bash
git add web/src/features/auth/pages/LoginPage.tsx
git commit -m "style: restyle login for Tahoe Control"
```

---

### Task 6: High-frequency pages layout pass

**Files (modify classNames / light structure only):**
- `web/src/features/dashboard/pages/PrincipalDashboardPage.tsx`
- `web/src/features/student/pages/StudentListPage.tsx`
- `web/src/features/student/components/StudentFilterBar.tsx`
- `web/src/features/student/components/StudentDetailSheet.tsx`
- `web/src/features/student/components/StudentFormDialog.tsx`
- `web/src/features/attendance/pages/AttendanceWorkbenchPage.tsx`
- `web/src/features/attendance/pages/LeaveListPage.tsx`
- `web/src/features/teacher/pages/WeeklySchedulePage.tsx`
- `web/src/features/teacher/pages/TeacherAvailabilityPage.tsx`
- `web/src/features/teacher/pages/MyWorkbenchPage.tsx`

- [ ] **Step 1: Page headers**

Unify titles: `text-2xl font-semibold tracking-tight` (drop conflicting heavy `font-serif` where it fights Tahoe Control, unless brand intentionally needs serif on one hero — default to sans).

- [ ] **Step 2: Filters / segmented controls**

For leave status tabs and similar, use gray track + white selected segment (warm text or warm fill for selected — prefer white pill on `bg-muted` track; selected label `text-foreground`, optional warm bottom indicator). Do not introduce system-blue fills.

- [ ] **Step 3: Cards / tables wrappers**

Wrap primary panels in `rounded-xl border border-border bg-white` (or `bg-card`).

- [ ] **Step 4: Build**

Run: `cd web && npm run build`  
Expected: success.

- [ ] **Step 5: Commit**

```bash
git add web/src/features/dashboard web/src/features/student web/src/features/attendance web/src/features/teacher
git commit -m "style: Tahoe layout pass on high-frequency pages"
```

---

### Task 7: Standard pages layout pass

**Files:**
- `web/src/features/course/pages/CourseListPage.tsx`
- `web/src/features/course/pages/ClassGroupListPage.tsx`
- `web/src/features/course/pages/ClassRoomListPage.tsx`
- `web/src/features/course/components/*FormDialog.tsx` (class names only)
- `web/src/features/settings/pages/BranchListPage.tsx`
- `web/src/features/settings/pages/UserListPage.tsx`
- `web/src/features/settings/pages/RoleListPage.tsx`
- Any remaining lesson schedule UI under `web/src/features/lesson/` if still reachable

- [ ] **Step 1: Apply same header/card/table patterns as Task 6**

No API or mutation changes.

- [ ] **Step 2: Build**

Run: `cd web && npm run build`  
Expected: success.

- [ ] **Step 3: Commit**

```bash
git add web/src/features/course web/src/features/settings web/src/features/lesson
git commit -m "style: Tahoe layout pass on remaining admin pages"
```

---

### Task 8: Deploy + regression verification

**Files:** none (ops)

- [ ] **Step 1: Package frontend into Spring static + rebuild app image**

```bash
cd web && npm run build
cd ..
rm -rf src/main/resources/static/*
cp -R web/dist/. src/main/resources/static/
mvn -q -DskipTests -Dskip.frontend.build package
cd docker && set -a && source .env && set +a
docker compose -f docker-compose.yml build app
docker compose -f docker-compose.yml up -d app
```

Expected: `eduze-app` healthy; `https://127.0.0.1:18443` serves new assets.

- [ ] **Step 2: Functional regression (admin)**

Login: `admin` / env `BOOTSTRAP_ADMIN_PASSWORD`  
Walk: 工作台 KPI → 学员分班展示 → 签到 → 请假 → 周课表 → 老师可用时段 → 课程/分组 → 设置页  
Expected: visuals match Tahoe Control + warm primary; no double nav highlight; dialogs close; no empty-state duplication.

- [ ] **Step 3: Mark spec acceptance checkboxes in the design doc or leave a short note in PR/commit body**

- [ ] **Step 4: Final commit if static packaging files changed unexpectedly**

Only commit if the repo tracks something beyond ignored `src/main/resources/static/**` (normally static is gitignored — do not force-add).

---

## Spec coverage check

| Spec requirement | Task |
|------------------|------|
| Tahoe Control chrome | 1–3 |
| Warm primary retained | 1–2 (explicit no blue) |
| Full-site pages | 5–7 |
| Dialog/table/empty rules | 4 |
| navItemEnd preserved | 3 |
| Login | 5 |
| Regression | 8 |
| No dark mode / no new UI lib | constraints in tasks 1–7 |

## Placeholder scan

None intentional. If a page file path differs slightly, search under `web/src/features/**/pages` and apply the same class patterns.
