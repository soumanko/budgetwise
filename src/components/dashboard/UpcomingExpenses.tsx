'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { formatCurrency } from '@/lib/utils';
import type { RecurringExpense } from '@/lib/types';
import { getCategoryDef } from '@/lib/finance/categories';
import { CalendarClock } from 'lucide-react';

interface UpcomingExpensesProps {
  expenses: RecurringExpense[];
  currency: string;
}

export function UpcomingExpenses({ expenses, currency }: UpcomingExpensesProps) {
  const now = new Date();
  const sorted = [...expenses]
    .filter(e => e.active)
    .sort((a, b) => new Date(a.next_due_date).getTime() - new Date(b.next_due_date).getTime())
    .slice(0, 5);

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <CalendarClock className="w-4 h-4 text-muted-foreground" />
          <CardTitle>Upcoming Expenses</CardTitle>
        </div>
      </CardHeader>
      <CardContent>
        {sorted.length === 0 ? (
          <p className="text-sm text-muted-foreground text-center py-4">
            No recurring expenses set up
          </p>
        ) : (
          <div className="space-y-3">
            {sorted.map((exp) => {
              const def = getCategoryDef(exp.category);
              const Icon = def.icon;
              const dueDate = new Date(exp.next_due_date);
              const daysUntil = Math.ceil((dueDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
              const dueLabel = daysUntil === 0 ? 'Due today' : daysUntil === 1 ? 'Due tomorrow' : daysUntil < 0 ? 'Overdue' : `In ${daysUntil} days`;
              const isDueSoon = daysUntil <= 3;

              return (
                <div key={exp.id} className="flex items-center gap-3">
                  <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${def.bgColor}`}>
                    <Icon className={`w-4 h-4 ${def.color}`} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium truncate">{exp.name}</p>
                    <p className={`text-xs ${isDueSoon ? 'text-amber-500 font-medium' : 'text-muted-foreground'}`}>
                      {dueLabel}
                    </p>
                  </div>
                  <span className="text-sm font-semibold">{formatCurrency(exp.amount, currency)}</span>
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
