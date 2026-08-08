'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { formatCurrency } from '@/lib/utils';
import { getCategoryDef } from '@/lib/finance/categories';
import type { CategorySpending } from '@/lib/types';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';

interface CategoryBreakdownProps {
  categories: CategorySpending[];
  currency: string;
}

const CHART_COLORS = [
  '#3b82f6', '#ef4444', '#f59e0b', '#10b981', '#8b5cf6',
  '#ec4899', '#06b6d4', '#f97316', '#6366f1', '#14b8a6',
  '#64748b', '#a855f7',
];

export function CategoryBreakdown({ categories, currency }: CategoryBreakdownProps) {
  const top = categories.slice(0, 6);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Spending by Category</CardTitle>
      </CardHeader>
      <CardContent>
        {categories.length === 0 ? (
          <div className="h-56 flex items-center justify-center text-sm text-muted-foreground">
            No expenses recorded yet
          </div>
        ) : (
          <div className="flex flex-col sm:flex-row items-center gap-4">
            {/* Donut chart */}
            <div className="w-36 h-36 shrink-0">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={top}
                    cx="50%"
                    cy="50%"
                    innerRadius={40}
                    outerRadius={65}
                    dataKey="amount"
                    nameKey="category"
                    strokeWidth={2}
                    stroke="hsl(var(--card))"
                  >
                    {top.map((_, i) => (
                      <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      backgroundColor: 'hsl(var(--card))',
                      border: '1px solid hsl(var(--border))',
                      borderRadius: '8px',
                      fontSize: '12px',
                    }}
                    formatter={(value) => formatCurrency(Number(value), currency)}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>

            {/* Category list */}
            <div className="flex-1 w-full space-y-2.5">
              {top.map((cat, i) => {
                const def = getCategoryDef(cat.category);
                const Icon = def.icon;
                return (
                  <div key={cat.category} className="flex items-center gap-3">
                    <div
                      className="w-2.5 h-2.5 rounded-full shrink-0"
                      style={{ backgroundColor: CHART_COLORS[i % CHART_COLORS.length] }}
                    />
                    <div className={`w-7 h-7 rounded-md flex items-center justify-center ${def.bgColor}`}>
                      <Icon className={`w-3.5 h-3.5 ${def.color}`} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium truncate">{cat.category}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-semibold">{formatCurrency(cat.amount, currency)}</p>
                      <p className="text-[11px] text-muted-foreground">{cat.percentage}%</p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
