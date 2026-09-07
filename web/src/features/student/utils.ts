export function calcAge(birthday?: string): string {
  if (!birthday) return '-';
  const b = new Date(birthday);
  if (Number.isNaN(b.getTime())) return '-';
  const now = new Date();
  let age = now.getFullYear() - b.getFullYear();
  const m = now.getMonth() - b.getMonth();
  if (m < 0 || (m === 0 && now.getDate() < b.getDate())) age -= 1;
  return `${age}岁`;
}

export function genderLabel(gender: number): string {
  if (gender === 1) return '男';
  if (gender === 2) return '女';
  return '未知';
}

export function statusLabel(status: number): string {
  if (status === 2) return '暂停';
  if (status === 3) return '退学';
  return '在读';
}

export function statusBadgeClass(status: number): string {
  if (status === 2) return 'bg-muted text-muted-fg';
  if (status === 3) return 'bg-muted text-muted-fg line-through';
  return 'bg-primary/10 text-primary';
}
