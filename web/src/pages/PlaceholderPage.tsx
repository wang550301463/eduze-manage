import { EmptyState } from '@/components/ui/EmptyState';

type PlaceholderPageProps = {
  title: string;
  description?: string;
};

export function PlaceholderPage({ title, description }: PlaceholderPageProps): JSX.Element {
  return (
    <EmptyState
      title={title}
      description={description ?? '该模块将在后续阶段实现'}
    />
  );
}
