import { Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

type Point = { week: string; rate: number };

type Props = {
  data: Point[];
};

export function StudentAttendanceTrend({ data }: Props): JSX.Element {
  return (
    <div className="h-48 w-full rounded-lg border border-border p-3">
      <p className="mb-2 text-sm font-medium">近 4 周出勤率</p>
      <ResponsiveContainer width="100%" height="85%">
        <BarChart data={data}>
          <XAxis dataKey="week" tick={{ fontSize: 11 }} />
          <YAxis domain={[0, 100]} tick={{ fontSize: 11 }} />
          <Tooltip formatter={(v: number) => [`${v}%`, '出勤率']} />
          <Bar dataKey="rate" fill="hsl(var(--primary))" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
