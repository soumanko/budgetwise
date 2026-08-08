'use client';

import { Card, CardContent } from '@/components/ui/card';
import { formatCurrency, percentChange } from '@/lib/utils';
import type { MonthlyStats } from '@/lib/types';
import { TrendingUp, TrendingDown, Wallet, ArrowUpRight, ArrowDownRight, PiggyBank } from 'lucide-react';

interface MonthlyOverviewProps {
  stats: MonthlyStats;
  previousStats: MonthlyStats;
  currency: string;
}

export function MonthlyOverview({ stats, previousStats, currency }: MonthlyOverviewProps) {
  const incomeChange = percentChange(stats.totalIncome, previousStats.totalIncome);
  const expenseChange = percentChange(stats.totalExpenses, previousStats.totalExpenses);
  const savingsChange = percentChange(stats.netSavings, previousStats.netSavings);

  const items = [
    {
      label: 'Income',
      value: stats.totalIncome,
      change: incomeChange,
      icon: ArrowDownRight,
      color: 'text-emerald-600 dark:text-emerald-400',
      bgColor: 'bg-emerald-500/10',
      positiveIsGood: true,
    },
    {
      label: 'Expenses',
      value: stats.totalExpenses,
      change: expenseChange,
      icon: ArrowUpRight,
      color: 'text-rose-600 dark:text-rose-400',
      bgColor: 'bg-rose-500/10',
      positiveIsGood: false,
    },
    {
      label: 'Savings',
      value: stats.netSavings,
      change: savingsChange,
      icon: PiggyBank,
      color: 'text-blue-600 dark:text-blue-400',
      bgColor: 'bg-blue-500/10',
      positiveIsGood: true,
      extra: stats.savingsRate > 0 ? `${stats.savingsRate}% saved` : undefined,
    },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      {items.map((item) => {
        const Icon = item.icon;
        const isPositive = item.change >= 0;
        const isGood = item.positiveIsGood ? isPositive : !isPositive;

        return (
          <Card key={item.label} className="hover:border-border/80 transition-colors">
            <CardContent className="p-4 lg:p-5">
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium text-muted-foreground">{item.label}</p>
                <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${item.bgColor}`}>
                  <Icon className={`w-4 h-4 ${item.color}`} />
                </div>
              </div>
              <p className={`text-xl lg:text-2xl font-bold mt-2 ${item.color}`}>
                {formatCurrency(item.value, currency)}
              </p>
              <div className="flex items-center gap-2 mt-1.5">
                {previousStats.transactionCount > 0 && (
                  <span className={`flex items-center gap-0.5 text-xs font-medium ${isGood ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'}`}>
                    {isPositive ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
                    {Math.abs(item.change).toFixed(1)}%
                  </span>
                )}
                {item.extra && (
                  <span className="text-xs text-muted-foreground">{item.extra}</span>
                )}
              </div>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
