// ============================================
// Deterministic Insight Generation
// Generates insights from raw financial data
// without requiring AI API calls
// ============================================

import type { Transaction, Budget, RecurringExpense } from '@/lib/types';
import {
  calculateMonthlyStats,
  calculateCategorySpending,
  predictMonthlyExpenses,
  toMoney,
} from './calculations';
import { getDaysInMonth } from '@/lib/utils';

export interface GeneratedInsight {
  type: string;
  title: string;
  description: string;
  severity: 'positive' | 'warning' | 'critical' | 'info';
  data?: Record<string, unknown>;
}

/**
 * Generate all deterministic insights from user data.
 */
export function generateInsights(params: {
  currentMonthTransactions: Transaction[];
  previousMonthTransactions: Transaction[];
  budgets: Budget[];
  recurringExpenses: RecurringExpense[];
  balance: number;
  lowBalanceThreshold: number;
}): GeneratedInsight[] {
  const insights: GeneratedInsight[] = [];
  const {
    currentMonthTransactions,
    previousMonthTransactions,
    budgets,
    recurringExpenses,
    balance,
    lowBalanceThreshold,
  } = params;

  const currentStats = calculateMonthlyStats(currentMonthTransactions);
  const previousStats = calculateMonthlyStats(previousMonthTransactions);
  const categorySpending = calculateCategorySpending(currentMonthTransactions);

  // 1. Savings achievement
  if (currentStats.netSavings > 0) {
    insights.push({
      type: 'savings_achievement',
      title: 'Savings this month',
      description: `You've saved ₹${currentStats.netSavings.toLocaleString('en-IN')} this month.`,
      severity: 'positive',
      data: { amount: currentStats.netSavings, rate: currentStats.savingsRate },
    });
  }

  // 2. Spending increase vs last month
  if (previousStats.totalExpenses > 0 && currentStats.totalExpenses > 0) {
    const change = ((currentStats.totalExpenses - previousStats.totalExpenses) / previousStats.totalExpenses) * 100;
    if (change > 15) {
      insights.push({
        type: 'spending_increase',
        title: 'Spending increased',
        description: `Your spending is ${Math.round(change)}% higher than last month.`,
        severity: 'warning',
        data: { change, current: currentStats.totalExpenses, previous: previousStats.totalExpenses },
      });
    } else if (change < -10) {
      insights.push({
        type: 'spending_decrease',
        title: 'Spending decreased',
        description: `Your spending is ${Math.abs(Math.round(change))}% lower than last month. Great job!`,
        severity: 'positive',
        data: { change, current: currentStats.totalExpenses, previous: previousStats.totalExpenses },
      });
    }
  }

  // 3. Category dominance
  if (categorySpending.length > 0 && categorySpending[0].percentage > 30) {
    const top = categorySpending[0];
    insights.push({
      type: 'category_dominance',
      title: `${top.category} is your top expense`,
      description: `${top.category} represents ${top.percentage}% of your spending this month.`,
      severity: 'info',
      data: { category: top.category, percentage: top.percentage, amount: top.amount },
    });
  }

  // 4. Budget warnings
  for (const budget of budgets) {
    const spent = categorySpending.find(c => c.category === budget.category)?.amount || 0;
    const utilization = budget.amount > 0 ? (spent / budget.amount) * 100 : 0;

    if (utilization > 100) {
      const overBy = toMoney(spent - budget.amount);
      insights.push({
        type: 'budget_exceeded',
        title: `${budget.category} budget exceeded`,
        description: `${budget.category} budget exceeded by ₹${overBy.toLocaleString('en-IN')}.`,
        severity: 'critical',
        data: { category: budget.category, spent, budget: budget.amount, utilization },
      });
    } else if (utilization >= 80) {
      insights.push({
        type: 'budget_warning',
        title: `${budget.category} budget almost full`,
        description: `Your ${budget.category} budget is ${Math.round(utilization)}% used.`,
        severity: 'warning',
        data: { category: budget.category, spent, budget: budget.amount, utilization },
      });
    }
  }

  // 5. Spending rate projection
  const now = new Date();
  const daysPassed = now.getDate();
  const totalDays = getDaysInMonth(now);

  if (daysPassed >= 3 && currentStats.totalExpenses > 0) {
    const projected = predictMonthlyExpenses(currentStats.totalExpenses, daysPassed, totalDays);
    insights.push({
      type: 'spending_projection',
      title: 'Projected monthly spending',
      description: `At your current rate, you may spend approximately ₹${projected.toLocaleString('en-IN')} this month.`,
      severity: projected > currentStats.totalIncome ? 'warning' : 'info',
      data: { projected, dailyRate: toMoney(currentStats.totalExpenses / daysPassed) },
    });
  }

  // 6. Low balance warning
  if (balance < lowBalanceThreshold && balance > 0) {
    insights.push({
      type: 'low_balance',
      title: 'Low balance alert',
      description: `Your balance is below ₹${lowBalanceThreshold.toLocaleString('en-IN')}.`,
      severity: 'critical',
      data: { balance, threshold: lowBalanceThreshold },
    });
  }

  // 7. Upcoming recurring expenses
  const upcoming = recurringExpenses
    .filter(r => r.active)
    .filter(r => {
      const dueDate = new Date(r.next_due_date);
      const daysUntil = Math.ceil((dueDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
      return daysUntil >= 0 && daysUntil <= 7;
    });

  if (upcoming.length > 0) {
    const totalUpcoming = upcoming.reduce((sum, r) => sum + r.amount, 0);
    insights.push({
      type: 'upcoming_recurring',
      title: 'Upcoming expenses',
      description: `₹${totalUpcoming.toLocaleString('en-IN')} in recurring expenses due within 7 days.`,
      severity: 'info',
      data: { count: upcoming.length, total: totalUpcoming },
    });
  }

  // 8. Weekend spending pattern
  const weekendExpenses = currentMonthTransactions
    .filter(t => t.type === 'expense')
    .filter(t => {
      const day = new Date(t.transaction_date + 'T00:00:00').getDay();
      return day === 0 || day === 6;
    });
  const weekdayExpenses = currentMonthTransactions
    .filter(t => t.type === 'expense')
    .filter(t => {
      const day = new Date(t.transaction_date + 'T00:00:00').getDay();
      return day !== 0 && day !== 6;
    });

  if (weekendExpenses.length >= 2 && weekdayExpenses.length >= 5) {
    const weekendTotal = weekendExpenses.reduce((s, t) => s + t.amount, 0);
    const weekdayTotal = weekdayExpenses.reduce((s, t) => s + t.amount, 0);
    const weekendAvg = weekendTotal / weekendExpenses.length;
    const weekdayAvg = weekdayTotal / weekdayExpenses.length;

    if (weekendAvg > weekdayAvg * 1.25) {
      const pctMore = Math.round(((weekendAvg - weekdayAvg) / weekdayAvg) * 100);
      insights.push({
        type: 'weekend_pattern',
        title: 'Weekend spending pattern',
        description: `You spend approximately ${pctMore}% more on weekends.`,
        severity: 'info',
        data: { weekendAvg, weekdayAvg, pctMore },
      });
    }
  }

  return insights;
}
