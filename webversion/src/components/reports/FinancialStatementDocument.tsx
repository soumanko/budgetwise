'use client';

import type { FinancialStatementData } from '@/lib/finance/statement';
import { formatCurrency, formatDate } from '@/lib/utils';
import { Badge } from '@/components/ui/badge';
import { getCategoryDef } from '@/lib/finance/categories';
import { Lightbulb } from 'lucide-react';

interface Props {
  data: FinancialStatementData;
}

export function FinancialStatementDocument({ data }: Props) {
  const c = data.currency;

  // ---- Empty state ----
  if (data.transactions.length === 0) {
    return (
      <div className="bg-card border border-border rounded-xl p-12 text-center">
        <p className="text-muted-foreground">No transactions found for the selected period.</p>
      </div>
    );
  }

  return (
    <div className="bg-card border border-border rounded-xl overflow-hidden">
      {/* ======== DOCUMENT HEADER ======== */}
      <div className="bg-primary/5 border-b border-border px-6 py-8 sm:px-10 sm:py-10">
        <div className="max-w-3xl">
          <p className="text-xs font-semibold tracking-widest text-primary uppercase mb-1">BudgetWise</p>
          <h2 className="text-2xl sm:text-3xl font-bold tracking-tight">Financial Statement</h2>
          <div className="mt-4 flex flex-wrap gap-x-6 gap-y-1 text-sm text-muted-foreground">
            <span>{formatDate(data.startDate)} → {formatDate(data.endDate)}</span>
            <span>Generated: {formatDate(data.generatedDate)}</span>
          </div>
        </div>
      </div>

      <div className="divide-y divide-border">
        {/* ======== FINANCIAL SUMMARY ======== */}
        <section className="px-6 py-8 sm:px-10">
          <h3 className="text-sm font-semibold tracking-widest text-muted-foreground uppercase mb-6">Financial Summary</h3>
          <div className="grid grid-cols-2 md:grid-cols-5 gap-6">
            <SummaryItem label="Opening Balance" value={formatCurrency(data.openingBalance, c)} />
            <SummaryItem label="Total Income" value={`+${formatCurrency(data.stats.totalIncome, c)}`} className="text-income" />
            <SummaryItem label="Total Expenses" value={`-${formatCurrency(data.stats.totalExpenses, c)}`} className="text-expense" />
            <SummaryItem
              label="Net Change"
              value={`${data.stats.netSavings >= 0 ? '+' : ''}${formatCurrency(data.stats.netSavings, c)}`}
              className={data.stats.netSavings >= 0 ? 'text-income' : 'text-expense'}
            />
            <SummaryItem label="Closing Balance" value={formatCurrency(data.closingBalance, c)} className="font-bold" />
          </div>
        </section>

        {/* ======== SPENDING OVERVIEW ======== */}
        <section className="px-6 py-8 sm:px-10">
          <h3 className="text-sm font-semibold tracking-widest text-muted-foreground uppercase mb-6">Spending Overview</h3>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-6">
            <OverviewItem label="Total Transactions" value={String(data.stats.transactionCount)} />
            <OverviewItem label="Expense Transactions" value={String(data.expenseCount)} />
            <OverviewItem label="Income Transactions" value={String(data.incomeCount)} />
            <OverviewItem label="Avg. Daily Spending" value={formatCurrency(data.averageDailySpending, c)} />
            <OverviewItem
              label="Largest Expense"
              value={data.largestExpense ? formatCurrency(data.largestExpense.amount, c) : '—'}
              detail={data.largestExpense ? (data.largestExpense.merchant || data.largestExpense.description || data.largestExpense.category) : undefined}
            />
          </div>
        </section>

        {/* ======== SPENDING BY CATEGORY ======== */}
        {data.categories.length > 0 && (
          <section className="px-6 py-8 sm:px-10">
            <h3 className="text-sm font-semibold tracking-widest text-muted-foreground uppercase mb-6">Spending by Category</h3>
            <div className="space-y-3">
              {data.categories.map((cat) => {
                const def = getCategoryDef(cat.category);
                const Icon = def.icon;
                const maxAmount = data.categories[0]?.amount || 1;
                const barWidth = (cat.amount / maxAmount) * 100;

                return (
                  <div key={cat.category} className="space-y-1.5">
                    <div className="flex items-center justify-between text-sm">
                      <div className="flex items-center gap-2.5">
                        <div className={`w-7 h-7 rounded-md flex items-center justify-center ${def.bgColor}`}>
                          <Icon className={`w-3.5 h-3.5 ${def.color}`} />
                        </div>
                        <span className="font-medium">{cat.category}</span>
                        <span className="text-xs text-muted-foreground">{cat.count} txns</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="text-xs text-muted-foreground">{cat.percentage}%</span>
                        <span className="font-semibold tabular-nums">{formatCurrency(cat.amount, c)}</span>
                      </div>
                    </div>
                    <div className="h-1.5 bg-muted rounded-full overflow-hidden">
                      <div
                        className="h-full bg-primary/70 rounded-full transition-all duration-500"
                        style={{ width: `${barWidth}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          </section>
        )}

        {/* ======== TRANSACTION DETAILS ======== */}
        <section className="px-6 py-8 sm:px-10">
          <h3 className="text-sm font-semibold tracking-widest text-muted-foreground uppercase mb-6">
            Transaction Details
            <span className="ml-2 text-xs font-normal text-muted-foreground">({data.transactions.length})</span>
          </h3>

          {/* Desktop table */}
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left">
                  <th className="pb-3 pr-4 font-medium text-muted-foreground text-xs uppercase tracking-wider">Date</th>
                  <th className="pb-3 pr-4 font-medium text-muted-foreground text-xs uppercase tracking-wider">Description</th>
                  <th className="pb-3 pr-4 font-medium text-muted-foreground text-xs uppercase tracking-wider">Category</th>
                  <th className="pb-3 pr-4 font-medium text-muted-foreground text-xs uppercase tracking-wider">Payment</th>
                  <th className="pb-3 font-medium text-muted-foreground text-xs uppercase tracking-wider text-right">Amount</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/50">
                {data.transactions.map((t) => (
                  <tr key={t.id} className="hover:bg-muted/30 transition-colors">
                    <td className="py-3 pr-4 whitespace-nowrap text-muted-foreground tabular-nums">{formatDate(t.transaction_date)}</td>
                    <td className="py-3 pr-4 font-medium">{t.merchant || t.description || t.category}</td>
                    <td className="py-3 pr-4">
                      <Badge variant="outline" className="font-normal text-xs">{t.category}</Badge>
                    </td>
                    <td className="py-3 pr-4 text-muted-foreground text-xs">{t.payment_method || '—'}</td>
                    <td className={`py-3 text-right font-semibold tabular-nums whitespace-nowrap ${t.type === 'income' ? 'text-income' : 'text-expense'}`}>
                      {t.type === 'income' ? '+' : '-'}{formatCurrency(t.amount, c)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile cards */}
          <div className="md:hidden space-y-2">
            {data.transactions.map((t) => (
              <div key={t.id} className="flex items-center justify-between py-3 border-b border-border/50 last:border-0">
                <div className="min-w-0 flex-1">
                  <p className="font-medium text-sm truncate">{t.merchant || t.description || t.category}</p>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className="text-xs text-muted-foreground">{formatDate(t.transaction_date)}</span>
                    <Badge variant="outline" className="font-normal text-[10px] px-1.5 py-0">{t.category}</Badge>
                  </div>
                </div>
                <span className={`text-sm font-semibold tabular-nums ml-3 ${t.type === 'income' ? 'text-income' : 'text-expense'}`}>
                  {t.type === 'income' ? '+' : '-'}{formatCurrency(t.amount, c)}
                </span>
              </div>
            ))}
          </div>
        </section>

        {/* ======== INSIGHTS ======== */}
        {data.insights.length > 0 && (
          <section className="px-6 py-8 sm:px-10">
            <h3 className="text-sm font-semibold tracking-widest text-muted-foreground uppercase mb-6 flex items-center gap-2">
              <Lightbulb className="w-4 h-4" />
              Insights
            </h3>
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {data.insights.map((insight, i) => (
                <div key={i} className="rounded-lg border border-border/50 bg-muted/30 px-4 py-3">
                  <p className="text-xs text-muted-foreground font-medium">{insight.label}</p>
                  <p className="text-sm font-semibold mt-0.5">{insight.value}</p>
                  {insight.detail && <p className="text-xs text-muted-foreground mt-1">{insight.detail}</p>}
                </div>
              ))}
            </div>
          </section>
        )}

        {/* ======== FOOTER ======== */}
        <div className="px-6 py-4 sm:px-10 bg-muted/30 text-center">
          <p className="text-xs text-muted-foreground">
            Generated by BudgetWise · All values are in {c} · This statement is for personal reference only
          </p>
        </div>
      </div>
    </div>
  );
}

// ---- Helper components ----

function SummaryItem({ label, value, className }: { label: string; value: string; className?: string }) {
  return (
    <div className="flex flex-col">
      <span className="text-xs font-medium text-muted-foreground mb-1">{label}</span>
      <span className={`text-lg sm:text-xl font-bold tabular-nums ${className || ''}`}>{value}</span>
    </div>
  );
}

function OverviewItem({ label, value, detail }: { label: string; value: string; detail?: string | null }) {
  return (
    <div className="flex flex-col">
      <span className="text-xs font-medium text-muted-foreground mb-1">{label}</span>
      <span className="text-base font-semibold tabular-nums">{value}</span>
      {detail && <span className="text-xs text-muted-foreground mt-0.5 truncate">{detail}</span>}
    </div>
  );
}
