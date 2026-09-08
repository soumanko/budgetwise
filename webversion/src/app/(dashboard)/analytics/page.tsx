'use client';

import { useState, useEffect, useCallback } from 'react';
import { createClient } from '@/lib/supabase/client';
import { useAuth } from '@/contexts/AuthContext';
import type { Transaction } from '@/lib/types';
import { calculateMonthlyStats, calculateCategorySpending, calculateDailySpending, toMoney } from '@/lib/finance/calculations';
import { formatCurrency, getMonthStart, getMonthEnd, getDaysInMonth, getPreviousMonthDateStr } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { Skeleton } from '@/components/ui/skeleton';
import { getCategoryDef } from '@/lib/finance/categories';
import { BarChart, Bar, LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, Legend } from 'recharts';
import { BarChart3, TrendingUp, TrendingDown, Calendar, DollarSign, ShoppingBag, ArrowUp } from 'lucide-react';

export default function AnalyticsPage() {
  const { user, profile } = useAuth();
  const supabase = createClient();

  const [currentTx, setCurrentTx] = useState<Transaction[]>([]);
  const [previousTx, setPreviousTx] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);

  const currency = profile?.currency || 'INR';

  const fetchData = useCallback(async () => {
    if (!user) return;
    const monthStart = getMonthStart();
    const monthEnd = getMonthEnd();
    const prevMonthStart = getPreviousMonthDateStr();
    const prevMonth = new Date();
    prevMonth.setMonth(prevMonth.getMonth() - 1);
    const prevMonthEnd = getMonthEnd(prevMonth);

    const [currentRes, prevRes] = await Promise.all([
      supabase.from('transactions').select('*').eq('user_id', user.id).gte('transaction_date', monthStart).lte('transaction_date', monthEnd),
      supabase.from('transactions').select('*').eq('user_id', user.id).gte('transaction_date', prevMonthStart).lte('transaction_date', prevMonthEnd),
    ]);

    setCurrentTx(currentRes.data || []);
    setPreviousTx(prevRes.data || []);
    setLoading(false);
  }, [user, supabase]);

  useEffect(() => { fetchData(); }, [fetchData]);

  if (loading) {
    return <div className="space-y-4"><Skeleton className="h-10 w-48" /><Skeleton className="h-96" /></div>;
  }

  const currentStats = calculateMonthlyStats(currentTx);
  const previousStats = calculateMonthlyStats(previousTx);
  const categorySpending = calculateCategorySpending(currentTx);
  const dailySpending = calculateDailySpending(currentTx);

  const avgDaily = currentTx.length > 0 ? toMoney(currentStats.totalExpenses / new Date().getDate()) : 0;
  const avgWeekly = toMoney(avgDaily * 7);
  const largestExpense = [...currentTx].filter(t => t.type === 'expense').sort((a, b) => b.amount - a.amount)[0];

  const chartData = dailySpending.map(d => ({
    date: d.date.slice(5),
    Expenses: d.amount,
    Income: d.income,
  }));

  const statCards = [
    { label: 'Total Spending', value: formatCurrency(currentStats.totalExpenses, currency), icon: DollarSign, color: 'text-rose-500', bg: 'bg-rose-500/10' },
    { label: 'Avg. Daily', value: formatCurrency(avgDaily, currency), icon: Calendar, color: 'text-blue-500', bg: 'bg-blue-500/10' },
    { label: 'Avg. Weekly', value: formatCurrency(avgWeekly, currency), icon: TrendingUp, color: 'text-purple-500', bg: 'bg-purple-500/10' },
    { label: 'Largest Expense', value: largestExpense ? formatCurrency(largestExpense.amount, currency) : '-', icon: ArrowUp, color: 'text-amber-500', bg: 'bg-amber-500/10', sub: largestExpense?.category },
  ];

  return (
    <div className="space-y-4 lg:space-y-6 animate-fade-in">
      <div>
        <h1 className="text-xl font-semibold">Analytics</h1>
        <p className="text-sm text-muted-foreground">Detailed breakdown of your finances this month</p>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {statCards.map((s) => {
          const Icon = s.icon;
          return (
            <Card key={s.label}>
              <CardContent className="p-4">
                <div className="flex items-center gap-2 mb-2">
                  <div className={`w-7 h-7 rounded-lg flex items-center justify-center ${s.bg}`}>
                    <Icon className={`w-3.5 h-3.5 ${s.color}`} />
                  </div>
                  <span className="text-xs text-muted-foreground">{s.label}</span>
                </div>
                <p className="text-lg font-bold">{s.value}</p>
                {s.sub && <p className="text-xs text-muted-foreground">{s.sub}</p>}
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* Income vs Expenses chart */}
      <Card>
        <CardHeader>
          <CardTitle>Income vs Expenses</CardTitle>
        </CardHeader>
        <CardContent>
          {chartData.length === 0 ? (
            <div className="h-64 flex items-center justify-center text-sm text-muted-foreground">No data yet</div>
          ) : (
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 5, right: 5, bottom: 0, left: -10 }}>
                  <CartesianGrid strokeDasharray="3 3" className="opacity-30" vertical={false} />
                  <XAxis dataKey="date" tick={{ fontSize: 11 }} tickLine={false} axisLine={false} />
                  <YAxis tick={{ fontSize: 11 }} tickLine={false} axisLine={false} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: 'hsl(var(--card))',
                      border: '1px solid hsl(var(--border))',
                      borderRadius: '8px',
                      fontSize: '12px',
                    }}
                    formatter={(value) => formatCurrency(Number(value), currency)}
                  />
                  <Legend />
                  <Bar dataKey="Income" fill="#10b981" radius={[3, 3, 0, 0]} maxBarSize={20} />
                  <Bar dataKey="Expenses" fill="#ef4444" radius={[3, 3, 0, 0]} maxBarSize={20} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Category comparison */}
      <Card>
        <CardHeader>
          <CardTitle>Category Spending</CardTitle>
        </CardHeader>
        <CardContent>
          {categorySpending.length === 0 ? (
            <div className="py-8 text-center text-sm text-muted-foreground">No expense data</div>
          ) : (
            <div className="space-y-3">
              {categorySpending.map((cat) => {
                const def = getCategoryDef(cat.category);
                const Icon = def.icon;
                const maxAmount = categorySpending[0]?.amount || 1;
                const barWidth = (cat.amount / maxAmount) * 100;

                return (
                  <div key={cat.category} className="space-y-1">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <div className={`w-6 h-6 rounded flex items-center justify-center ${def.bgColor}`}>
                          <Icon className={`w-3 h-3 ${def.color}`} />
                        </div>
                        <span className="text-sm font-medium">{cat.category}</span>
                        <span className="text-xs text-muted-foreground">{cat.count} transactions</span>
                      </div>
                      <div className="text-right">
                        <span className="text-sm font-semibold">{formatCurrency(cat.amount, currency)}</span>
                        <span className="text-xs text-muted-foreground ml-1.5">{cat.percentage}%</span>
                      </div>
                    </div>
                    <div className="h-1.5 bg-muted rounded-full overflow-hidden">
                      <div
                        className="h-full bg-primary rounded-full transition-all duration-500"
                        style={{ width: `${barWidth}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Monthly comparison */}
      <Card>
        <CardHeader>
          <CardTitle>Month-over-Month</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <p className="text-xs text-muted-foreground font-medium">This Month</p>
              <p className="text-sm">Income: <span className="font-semibold text-emerald-600 dark:text-emerald-400">{formatCurrency(currentStats.totalIncome, currency)}</span></p>
              <p className="text-sm">Expenses: <span className="font-semibold text-rose-600 dark:text-rose-400">{formatCurrency(currentStats.totalExpenses, currency)}</span></p>
              <p className="text-sm">Savings: <span className="font-semibold">{formatCurrency(currentStats.netSavings, currency)}</span></p>
              <p className="text-sm">Rate: <span className="font-semibold">{currentStats.savingsRate}%</span></p>
            </div>
            <div className="space-y-2">
              <p className="text-xs text-muted-foreground font-medium">Last Month</p>
              <p className="text-sm">Income: <span className="font-semibold text-emerald-600 dark:text-emerald-400">{formatCurrency(previousStats.totalIncome, currency)}</span></p>
              <p className="text-sm">Expenses: <span className="font-semibold text-rose-600 dark:text-rose-400">{formatCurrency(previousStats.totalExpenses, currency)}</span></p>
              <p className="text-sm">Savings: <span className="font-semibold">{formatCurrency(previousStats.netSavings, currency)}</span></p>
              <p className="text-sm">Rate: <span className="font-semibold">{previousStats.savingsRate}%</span></p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
