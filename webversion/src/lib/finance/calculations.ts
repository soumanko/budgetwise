// ============================================
// Financial Calculations Engine
// All money operations use precise arithmetic
// ============================================

import type { Transaction, MonthlyStats, CategorySpending, DailySpending } from '@/lib/types';

/**
 * Safely parse a monetary value, avoiding floating point issues.
 * Rounds to 2 decimal places.
 */
export function toMoney(value: number | string): number {
  const num = typeof value === 'string' ? parseFloat(value) : value;
  return Math.round(num * 100) / 100;
}

/**
 * Add two monetary values precisely.
 */
export function addMoney(a: number, b: number): number {
  return Math.round((a * 100 + b * 100)) / 100;
}

/**
 * Subtract two monetary values precisely.
 */
export function subtractMoney(a: number, b: number): number {
  return Math.round((a * 100 - b * 100)) / 100;
}

/**
 * Calculate balance from transactions.
 * Balance = Total Income - Total Expenses
 */
export function calculateBalance(transactions: Transaction[]): number {
  let balanceCents = 0;
  for (const t of transactions) {
    const amountCents = Math.round(t.amount * 100);
    if (t.type === 'income') {
      balanceCents += amountCents;
    } else {
      balanceCents -= amountCents;
    }
  }
  return balanceCents / 100;
}

/**
 * Calculate monthly statistics from transactions within a date range.
 */
export function calculateMonthlyStats(transactions: Transaction[]): MonthlyStats {
  let incomeCents = 0;
  let expenseCents = 0;

  for (const t of transactions) {
    const amountCents = Math.round(t.amount * 100);
    if (t.type === 'income') {
      incomeCents += amountCents;
    } else {
      expenseCents += amountCents;
    }
  }

  const totalIncome = incomeCents / 100;
  const totalExpenses = expenseCents / 100;
  const netSavings = (incomeCents - expenseCents) / 100;
  const savingsRate = totalIncome > 0 ? (netSavings / totalIncome) * 100 : 0;

  return {
    totalIncome,
    totalExpenses,
    netSavings,
    savingsRate: Math.round(savingsRate * 10) / 10,
    transactionCount: transactions.length,
  };
}

/**
 * Calculate spending breakdown by category.
 */
export function calculateCategorySpending(transactions: Transaction[]): CategorySpending[] {
  const expenses = transactions.filter(t => t.type === 'expense');
  const categoryMap = new Map<string, { amount: number; count: number }>();

  let totalCents = 0;
  for (const t of expenses) {
    const amountCents = Math.round(t.amount * 100);
    totalCents += amountCents;
    const existing = categoryMap.get(t.category) || { amount: 0, count: 0 };
    existing.amount += amountCents;
    existing.count += 1;
    categoryMap.set(t.category, existing);
  }

  const result: CategorySpending[] = [];
  for (const [category, data] of categoryMap.entries()) {
    result.push({
      category,
      amount: data.amount / 100,
      percentage: totalCents > 0 ? Math.round((data.amount / totalCents) * 1000) / 10 : 0,
      count: data.count,
    });
  }

  return result.sort((a, b) => b.amount - a.amount);
}

/**
 * Calculate daily spending data for charts.
 */
export function calculateDailySpending(transactions: Transaction[]): DailySpending[] {
  const dailyMap = new Map<string, { expense: number; income: number }>();

  for (const t of transactions) {
    const date = t.transaction_date;
    const existing = dailyMap.get(date) || { expense: 0, income: 0 };
    const amountCents = Math.round(t.amount * 100);
    if (t.type === 'expense') {
      existing.expense += amountCents;
    } else {
      existing.income += amountCents;
    }
    dailyMap.set(date, existing);
  }

  const result: DailySpending[] = [];
  for (const [date, data] of dailyMap.entries()) {
    result.push({
      date,
      amount: data.expense / 100,
      income: data.income / 100,
    });
  }

  return result.sort((a, b) => a.date.localeCompare(b.date));
}

/**
 * Calculate "Safe to Spend" — daily budget based on remaining balance and days in month.
 */
export function calculateSafeToSpend(
  balance: number,
  daysRemaining: number,
  upcomingRecurringTotal: number,
  monthlyBudget: number
): number {
  if (daysRemaining <= 0) return 0;

  // Available = balance - upcoming recurring expenses
  const available = subtractMoney(balance, upcomingRecurringTotal);

  // If there's a monthly budget, use the lesser of available and remaining budget
  let effectiveAvailable = available;
  if (monthlyBudget > 0) {
    effectiveAvailable = Math.min(available, monthlyBudget);
  }

  if (effectiveAvailable <= 0) return 0;

  return toMoney(effectiveAvailable / daysRemaining);
}

/**
 * Predict end-of-month expenses based on current spending rate.
 */
export function predictMonthlyExpenses(
  currentExpenses: number,
  daysPassed: number,
  totalDaysInMonth: number
): number {
  if (daysPassed <= 0) return 0;
  const dailyRate = currentExpenses / daysPassed;
  return toMoney(dailyRate * totalDaysInMonth);
}

/**
 * Predict end-of-month balance.
 */
export function predictEndOfMonthBalance(
  currentBalance: number,
  predictedRemainingExpenses: number,
  expectedRemainingIncome: number
): number {
  return toMoney(currentBalance - predictedRemainingExpenses + expectedRemainingIncome);
}

/**
 * Calculate financial health score (0-100).
 */
export function calculateFinancialHealthScore(params: {
  savingsRate: number; // percentage
  budgetAdherence: number; // percentage of budgets under limit
  spendingConsistency: number; // lower is better (std deviation / avg daily spend)
  hasEmergencyFund: boolean;
  overBudgetCategories: number;
  totalCategories: number;
}): { score: number; factors: { label: string; status: 'good' | 'warning' | 'bad'; detail: string }[] } {
  let score = 50; // base
  const factors: { label: string; status: 'good' | 'warning' | 'bad'; detail: string }[] = [];

  // Savings rate (0-25 points)
  if (params.savingsRate >= 30) {
    score += 25;
    factors.push({ label: 'Savings rate', status: 'good', detail: `Your savings rate is ${params.savingsRate.toFixed(1)}%` });
  } else if (params.savingsRate >= 15) {
    score += 15;
    factors.push({ label: 'Savings rate', status: 'good', detail: `Your savings rate is ${params.savingsRate.toFixed(1)}%` });
  } else if (params.savingsRate >= 0) {
    score += 5;
    factors.push({ label: 'Savings rate', status: 'warning', detail: `Your savings rate is only ${params.savingsRate.toFixed(1)}%` });
  } else {
    score -= 10;
    factors.push({ label: 'Savings rate', status: 'bad', detail: `You're spending more than you earn` });
  }

  // Budget adherence (0-25 points)
  if (params.budgetAdherence >= 90) {
    score += 25;
    factors.push({ label: 'Budget adherence', status: 'good', detail: 'You stayed within your budgets' });
  } else if (params.budgetAdherence >= 70) {
    score += 15;
    factors.push({ label: 'Budget adherence', status: 'warning', detail: 'Some budgets were exceeded' });
  } else {
    score += 0;
    factors.push({ label: 'Budget adherence', status: 'bad', detail: 'Multiple budgets exceeded' });
  }

  // Over-budget categories penalty
  if (params.overBudgetCategories > 0) {
    score -= params.overBudgetCategories * 3;
    factors.push({
      label: 'Over-budget categories',
      status: 'warning',
      detail: `${params.overBudgetCategories} category budget${params.overBudgetCategories > 1 ? 's' : ''} exceeded`
    });
  }

  return {
    score: Math.max(0, Math.min(100, Math.round(score))),
    factors,
  };
}

/**
 * Calculate date when balance might drop below threshold.
 */
export function predictLowBalanceDate(
  currentBalance: number,
  avgDailyExpenses: number,
  threshold: number
): Date | null {
  if (avgDailyExpenses <= 0 || currentBalance <= threshold) return null;

  const daysUntilThreshold = (currentBalance - threshold) / avgDailyExpenses;
  const date = new Date();
  date.setDate(date.getDate() + Math.floor(daysUntilThreshold));

  return date;
}
