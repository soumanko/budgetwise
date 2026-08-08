'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { formatCurrency } from '@/lib/utils';
import type { Budget, CategorySpending } from '@/lib/types';
import { getCategoryDef } from '@/lib/finance/categories';

interface BudgetOverviewProps {
  budgets: Budget[];
  categorySpending: CategorySpending[];
  currency: string;
}

export function BudgetOverview({ budgets, categorySpending, currency }: BudgetOverviewProps) {
  if (budgets.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Budget Overview</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col items-center justify-center py-6 text-center">
            <p className="text-sm text-muted-foreground mb-1">No budgets set</p>
            <p className="text-xs text-muted-foreground">Create category budgets to track your spending limits</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Budget Overview</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {budgets.slice(0, 5).map((budget) => {
          const spent = categorySpending.find(c => c.category === budget.category)?.amount || 0;
          const utilization = budget.amount > 0 ? Math.min((spent / budget.amount) * 100, 150) : 0;
          const isOver = spent > budget.amount;
          const def = getCategoryDef(budget.category);
          const Icon = def.icon;

          return (
            <div key={budget.id} className="space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className={`w-6 h-6 rounded-md flex items-center justify-center ${def.bgColor}`}>
                    <Icon className={`w-3 h-3 ${def.color}`} />
                  </div>
                  <span className="text-sm font-medium">{budget.category}</span>
                </div>
                <div className="text-right">
                  <span className={`text-sm font-semibold ${isOver ? 'text-rose-500' : ''}`}>
                    {formatCurrency(spent, currency)}
                  </span>
                  <span className="text-sm text-muted-foreground"> / {formatCurrency(budget.amount, currency)}</span>
                </div>
              </div>
              <Progress
                value={Math.min(utilization, 100)}
                indicatorClassName={
                  isOver
                    ? 'bg-rose-500'
                    : utilization >= 80
                    ? 'bg-amber-500'
                    : 'bg-emerald-500'
                }
              />
              {isOver && (
                <p className="text-xs text-rose-500 font-medium">
                  Over by {formatCurrency(spent - budget.amount, currency)}
                </p>
              )}
            </div>
          );
        })}
      </CardContent>
    </Card>
  );
}
