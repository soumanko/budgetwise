'use client';

import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { formatCurrency, percentChange } from '@/lib/utils';
import { TrendingUp, TrendingDown, Plus, Minus, ArrowUpRight, ArrowDownRight } from 'lucide-react';

interface BalanceCardProps {
  balance: number;
  totalIncome: number;
  totalExpenses: number;
  previousExpenses: number;
  currency: string;
  onAddExpense: () => void;
  onAddIncome: () => void;
}

export function BalanceCard({ balance, totalIncome, totalExpenses, previousExpenses, currency, onAddExpense, onAddIncome }: BalanceCardProps) {
  const expenseChange = percentChange(totalExpenses, previousExpenses);

  return (
    <Card className="relative overflow-hidden">
      <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-transparent" />
      <CardContent className="relative p-5 lg:p-6">
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
          <div className="space-y-1">
            <p className="text-sm font-medium text-muted-foreground">Current Balance</p>
            <p className={`text-3xl lg:text-4xl font-bold tracking-tight ${balance >= 0 ? 'text-foreground' : 'text-destructive'}`}>
              {formatCurrency(balance, currency)}
            </p>
            <div className="flex items-center gap-4 pt-2">
              <div className="flex items-center gap-1.5">
                <div className="w-6 h-6 rounded-full bg-emerald-500/10 flex items-center justify-center">
                  <ArrowDownRight className="w-3.5 h-3.5 text-emerald-500" />
                </div>
                <span className="text-sm text-muted-foreground">Income</span>
                <span className="text-sm font-semibold text-emerald-600 dark:text-emerald-400">
                  {formatCurrency(totalIncome, currency)}
                </span>
              </div>
              <div className="flex items-center gap-1.5">
                <div className="w-6 h-6 rounded-full bg-rose-500/10 flex items-center justify-center">
                  <ArrowUpRight className="w-3.5 h-3.5 text-rose-500" />
                </div>
                <span className="text-sm text-muted-foreground">Expenses</span>
                <span className="text-sm font-semibold text-rose-600 dark:text-rose-400">
                  {formatCurrency(totalExpenses, currency)}
                </span>
              </div>
            </div>

            {previousExpenses > 0 && (
              <div className="pt-1">
                <Badge variant={expenseChange > 0 ? 'expense' : 'income'} className="text-[11px]">
                  {expenseChange > 0 ? <TrendingUp className="w-3 h-3 mr-1" /> : <TrendingDown className="w-3 h-3 mr-1" />}
                  {Math.abs(expenseChange).toFixed(1)}% vs last month
                </Badge>
              </div>
            )}
          </div>

          <div className="flex sm:flex-col gap-2">
            <Button onClick={onAddIncome} variant="income" size="sm" className="flex-1 sm:flex-none">
              <Plus className="w-4 h-4" />
              Add Money
            </Button>
            <Button onClick={onAddExpense} variant="expense" size="sm" className="flex-1 sm:flex-none">
              <Minus className="w-4 h-4" />
              Add Expense
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
