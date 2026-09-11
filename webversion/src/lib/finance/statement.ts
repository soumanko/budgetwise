// ============================================
// Financial Statement — Data Contract & Builder
// ============================================
//
// All calculations are deterministic. No LLM involvement.
// Reuses existing precise-arithmetic functions from calculations.ts.
//
// Statement Data Contract (shared semantics with Android):
//   Date boundaries: inclusive [startDate, endDate]
//   Income: type === 'income', amount is positive
//   Expense: type === 'expense', amount is positive (displayed with - prefix)
//   Opening Balance: calculateBalance(all transactions before startDate)
//   Closing Balance: openingBalance + totalIncome - totalExpenses
//   Net Change: totalIncome - totalExpenses
//   Category aggregation: expense transactions grouped by category, sorted desc by amount
//   Transaction ordering: ascending by transaction_date
// ============================================

import type { Transaction, MonthlyStats, CategorySpending, DailySpending } from '@/lib/types';
import {
  calculateMonthlyStats,
  calculateCategorySpending,
  calculateDailySpending,
  toMoney,
} from './calculations';
import { formatCurrency } from '../utils';

// ---- Interfaces ----

export interface StatementInsight {
  label: string;
  value: string;
  detail?: string;
}

export interface FinancialStatementData {
  // Period
  startDate: string;
  endDate: string;
  generatedDate: string;

  // Balances
  openingBalance: number;
  closingBalance: number;

  // Period metrics (from calculateMonthlyStats)
  stats: MonthlyStats;

  // Category breakdown (from calculateCategorySpending)
  categories: CategorySpending[];

  // Daily breakdown (from calculateDailySpending)
  dailySpending: DailySpending[];

  // Transactions (the period dataset, ordered ascending by date)
  transactions: Transaction[];

  // Spending overview
  expenseCount: number;
  incomeCount: number;
  averageDailySpending: number;
  largestExpense: Transaction | null;

  // Deterministic insights
  insights: StatementInsight[];

  // Currency
  currency: string;
}

// ---- Builder ----

/**
 * Build a complete FinancialStatementData from fetched data.
 * This is a pure function: no Supabase calls, no side effects.
 *
 * @param periodTransactions - Transactions within [startDate, endDate], ordered asc by date
 * @param openingBalance - calculateBalance(all transactions before startDate)
 * @param startDate - inclusive start (YYYY-MM-DD)
 * @param endDate - inclusive end (YYYY-MM-DD)
 * @param currency - e.g. 'INR'
 */
export function buildFinancialStatement(
  periodTransactions: Transaction[],
  openingBalance: number,
  startDate: string,
  endDate: string,
  currency: string,
): FinancialStatementData {
  const stats = calculateMonthlyStats(periodTransactions);
  const categories = calculateCategorySpending(periodTransactions);
  const dailySpending = calculateDailySpending(periodTransactions);

  const closingBalance = toMoney(openingBalance + stats.netSavings);

  const expenses = periodTransactions.filter(t => t.type === 'expense');
  const incomes = periodTransactions.filter(t => t.type === 'income');

  const largestExpense = expenses.length > 0
    ? expenses.reduce((max, t) => t.amount > max.amount ? t : max, expenses[0])
    : null;

  // Days in range (inclusive)
  const start = new Date(startDate + 'T00:00:00');
  const end = new Date(endDate + 'T00:00:00');
  const daysInRange = Math.max(1, Math.floor((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1);

  const averageDailySpending = expenses.length > 0
    ? toMoney(stats.totalExpenses / daysInRange)
    : 0;

  const today = new Date();
  const generatedDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

  const insights = calculateStatementInsights(
    periodTransactions,
    categories,
    dailySpending,
    stats,
    daysInRange,
    largestExpense,
    currency,
  );

  return {
    startDate,
    endDate,
    generatedDate,
    openingBalance,
    closingBalance,
    stats,
    categories,
    dailySpending,
    transactions: periodTransactions,
    expenseCount: expenses.length,
    incomeCount: incomes.length,
    averageDailySpending,
    largestExpense,
    insights,
    currency,
  };
}

// ---- Insights ----

/**
 * Derive all deterministic insights from statement data.
 * No LLM. No guessing. Pure data.
 */
function calculateStatementInsights(
  transactions: Transaction[],
  categories: CategorySpending[],
  dailySpending: DailySpending[],
  stats: MonthlyStats,
  daysInRange: number,
  largestExpense: Transaction | null,
  currency: string,
): StatementInsight[] {
  const insights: StatementInsight[] = [];

  // Highest spending category
  if (categories.length > 0) {
    const top = categories[0];
    insights.push({
      label: 'Highest spending category',
      value: top.category,
      detail: `${formatCurrency(top.amount, currency)} · ${top.percentage}% · ${top.count} transactions`,
    });
  }

  // Highest spending day
  if (dailySpending.length > 0) {
    const topDay = [...dailySpending].sort((a, b) => b.amount - a.amount)[0];
    if (topDay.amount > 0) {
      // Find exact transaction count for this day
      const dayCount = transactions.filter(t => t.type === 'expense' && t.transaction_date === topDay.date).length;
      
      const d = new Date(topDay.date + 'T00:00:00');
      const dateStr = d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
      
      insights.push({
        label: 'Highest spending day',
        value: dateStr,
        detail: `${formatCurrency(topDay.amount, currency)} · ${dayCount} transactions`,
      });
    }
  }

  // Largest individual expense
  if (largestExpense) {
    const desc = largestExpense.merchant || largestExpense.description || largestExpense.category;
    const d = new Date(largestExpense.transaction_date + 'T00:00:00');
    const dateStr = d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
    insights.push({
      label: 'Largest expense',
      value: desc,
      detail: `${dateStr} · ${formatCurrency(largestExpense.amount, currency)}`,
    });
  }

  // Expense days count
  const expenseDays = dailySpending.filter(d => d.amount > 0).length;
  if (daysInRange > 0) {
    insights.push({
      label: 'Expense days',
      value: `${expenseDays} of ${daysInRange} days`,
      detail: `${Math.round((expenseDays / daysInRange) * 100)}%`,
    });
  }

  // Average expense size
  const expenses = transactions.filter(t => t.type === 'expense');
  if (expenses.length > 0) {
    const avgExpense = stats.totalExpenses / expenses.length;
    insights.push({
      label: 'Average expense',
      value: formatCurrency(avgExpense, currency),
      detail: `${expenses.length} expenses`,
    });
  }

  // Top category percentage
  if (categories.length >= 2) {
    const topTwoPercent = toMoney(categories[0].percentage + categories[1].percentage);
    insights.push({
      label: 'Top categories',
      value: `${categories[0].category} + ${categories[1].category}`,
      detail: `${topTwoPercent}% of spending`,
    });
  }

  return insights;
}
