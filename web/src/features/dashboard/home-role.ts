export function prefersTeachingHome(roles: string[]): boolean {
  return (
    roles.includes('TEACHER') && !roles.some((role) => ['SUPER_ADMIN', 'PRINCIPAL'].includes(role))
  );
}
