import {
  flexRender,
  getCoreRowModel,
  useReactTable,
  type ColumnDef,
} from '@tanstack/react-table';
import { cn } from '@/lib/cn';
import { EmptyState } from './EmptyState';
import { Pagination } from './Pagination';
import { Skeleton } from './Skeleton';
import type { DataTableProps } from './DataTable.types';

const SKELETON_ROWS = 5;

export function DataTable<T>({
  columns,
  data,
  loading = false,
  empty,
  pageState,
  rowKey,
  onRowClick,
  toolbar,
  stickyHeader = false,
  mobileCardRender,
}: DataTableProps<T>): JSX.Element {
  const table = useReactTable({
    data,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getRowId: (row) => String(rowKey(row)),
  });

  const showEmpty = !loading && data.length === 0;
  const emptyContent = empty ?? (
    <EmptyState title="暂无数据" description="当前列表为空" />
  );

  return (
    <div className="space-y-4">
      {toolbar ? <div className="flex flex-wrap items-center gap-2">{toolbar}</div> : null}

      {showEmpty ? (
        <div className="rounded-xl bg-card shadow-sm">{emptyContent}</div>
      ) : (
        <>
          {/* Desktop table */}
          <div className="hidden overflow-x-auto rounded-lg border border-border md:block">
            <table className="w-full border-collapse text-sm">
              <thead
                className={cn(
                  stickyHeader && 'sticky top-0 z-10 bg-background shadow-sm',
                )}
              >
                {table.getHeaderGroups().map((hg) => (
                  <tr key={hg.id} className="border-b border-border">
                    {hg.headers.map((header) => (
                      <th
                        key={header.id}
                        className="px-4 py-3 text-left font-medium text-muted-fg"
                      >
                        {header.isPlaceholder
                          ? null
                          : flexRender(header.column.columnDef.header, header.getContext())}
                      </th>
                    ))}
                  </tr>
                ))}
              </thead>
              <tbody>
                {loading
                  ? Array.from({ length: SKELETON_ROWS }).map((_, i) => (
                      <tr key={`sk-${i}`} className="border-b border-border">
                        {columns.map((_, ci) => (
                          <td key={ci} className="px-4 py-3">
                            <Skeleton className="h-4 w-full" />
                          </td>
                        ))}
                      </tr>
                    ))
                  : null}
                {!loading
                  ? table.getRowModel().rows.map((row) => (
                      <tr
                        key={row.id}
                        className={cn(
                          'border-b border-border transition-colors',
                          onRowClick && 'cursor-pointer hover:bg-muted/40',
                        )}
                        onClick={() => onRowClick?.(row.original)}
                      >
                        {row.getVisibleCells().map((cell) => (
                          <td key={cell.id} className="px-4 py-3">
                            {flexRender(cell.column.columnDef.cell, cell.getContext())}
                          </td>
                        ))}
                      </tr>
                    ))
                  : null}
              </tbody>
            </table>
          </div>

          {/* Mobile cards */}
          <div className="space-y-3 md:hidden">
            {loading
              ? Array.from({ length: SKELETON_ROWS }).map((_, i) => (
                  <Skeleton key={i} className="h-20 w-full rounded-lg" />
                ))
              : null}
            {!loading && mobileCardRender
              ? data.map((row) => (
                  <div
                    key={rowKey(row)}
                    className={cn(
                      'rounded-lg border border-border bg-background p-4 shadow-sm',
                      onRowClick && 'cursor-pointer active:bg-muted/40',
                    )}
                    onClick={() => onRowClick?.(row)}
                  >
                    {mobileCardRender(row)}
                  </div>
                ))
              : null}
            {!loading && !mobileCardRender
              ? table.getRowModel().rows.map((row) => (
                  <div
                    key={row.id}
                    className="rounded-lg border border-border bg-background p-4"
                    onClick={() => onRowClick?.(row.original)}
                  >
                    {row.getVisibleCells().map((cell) => (
                      <div key={cell.id} className="flex justify-between gap-2 py-1">
                        <span className="text-muted-fg">
                          {typeof cell.column.columnDef.header === 'string'
                            ? cell.column.columnDef.header
                            : cell.column.id}
                        </span>
                        <span>{flexRender(cell.column.columnDef.cell, cell.getContext())}</span>
                      </div>
                    ))}
                  </div>
                ))
              : null}
          </div>
        </>
      )}

      {pageState ? (
        <Pagination
          page={pageState.page}
          pageSize={pageState.pageSize}
          total={pageState.total}
          onChange={pageState.onChange}
        />
      ) : null}
    </div>
  );
}

export type { ColumnDef, DataTableProps };
