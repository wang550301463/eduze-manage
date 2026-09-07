import type { ColumnDef } from '@tanstack/react-table';
import type { ReactNode } from 'react';

export type PageState = {
  page: number;
  pageSize: number;
  total: number;
  onChange: (next: { page: number; pageSize: number }) => void;
};

export type DataTableProps<T> = {
  columns: ColumnDef<T>[];
  data: T[];
  loading?: boolean;
  empty?: ReactNode;
  pageState?: PageState;
  rowKey: (row: T) => string | number;
  onRowClick?: (row: T) => void;
  toolbar?: ReactNode;
  stickyHeader?: boolean;
  mobileCardRender?: (row: T) => ReactNode;
};
