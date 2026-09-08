'use client';

import { useState, useEffect, useCallback } from 'react';
import { createClient } from '@/lib/supabase/client';
import { useAuth } from '@/contexts/AuthContext';
import type { Transaction, Budget, RecurringExpense, MonthlyStats, CategorySpending } from '@/lib/types';
import { calculateBalance, calculateMonthlyStats, calculateCategorySpending, calculateSafeToSpend, predictMonthlyExpenses, calculateFinancialHealthScore, calculateDailySpending, toMoney } from '@/lib/finance/calculations';
import { generateInsights, type GeneratedInsight } from '@/lib/finance/insights';
import { getMonthStart, getMonthEnd, getDaysRemainingInMonth, getDaysInMonth, getPreviousMonthDateStr, formatCurrency } from '@/lib/utils';
import { BalanceCard } from '@/components/dashboard/BalanceCard';
import { SafeToSpendCard } from '@/components/dashboard/SafeToSpendCard';
import { MonthlyOverview } from '@/components/dashboard/MonthlyOverview';
import { SpendingChart } from '@/components/dashboard/SpendingChart';
import { CategoryBreakdown } from '@/components/dashboard/CategoryBreakdown';
import { BudgetOverview } from '@/components/dashboard/BudgetOverview';
import { SmartInsights } from '@/components/dashboard/SmartInsights';
import { RecentTransactions } from '@/components/dashboard/RecentTransactions';
import { FinancialHealthScore } from '@/components/dashboard/FinancialHealthScore';
import { UpcomingExpenses } from '@/components/dashboard/UpcomingExpenses';
import { EmptyDashboard } from '@/components/dashboard/EmptyDashboard';
import { AddExpenseModal } from '@/components/transactions/AddExpenseModal';
import { AddIncomeModal } from '@/components/transactions/AddIncomeModal';
import { Skeleton } from '@/components/ui/skeleton';

export default function DashboardPage() {
  const { user, profile, accounts } = useAuth();
  const supabase = createClient();

  const [allTransactions, setAllTransactions] = useState<Transaction[]>([]);
  const [currentMonthTx, setCurrentMonthTx] = useState<Transaction[]>([]);
  const [previousMonthTx, setPreviousMonthTx] = useState<Transaction[]>([]);
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [recurringExpenses, setRecurringExpenses] = useState<RecurringExpense[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAddExpense, setShowAddExpense] = useState(false);
  const [showAddIncome, setShowAddIncome] = useState(false);

  const fetchData = useCallback(async () => {
    if (!user) return;

    const monthStart = getMonthStart();
    const monthEnd = getMonthEnd();
    const prevMonthStart = getPreviousMonthDateStr();
    const prevMonth = new Date();
    prevMonth.setMonth(prevMonth.getMonth() - 1);
    const prevMonthEnd = getMonthEnd(prevMonth);

    const [txRes, currentTxRes, prevTxRes, budgetRes, recurringRes] = await Promise.all([
      supabase.from('transactions').select('*').eq('user_id', user.id).order('transaction_date', { ascending: false }),
      supabase.from('transactions').select('*').eq('user_id', user.id).gte('transaction_date', monthStart).lte('transaction_date', monthEnd),
      supabase.from('transactions').select('*').eq('user_id', user.id).gte('transaction_date', prevMonthStart).lte('transaction_date', prevMonthEnd),
      supabase.from('budgets').select('*').eq('user_id', user.id).eq('month', monthStart),
      supabase.from('recurring_expenses').select('*').eq('user_id', user.id).eq('active', true),
    ]);

    setAllTransactions(txRes.data || []);
    setCurrentMonthTx(currentTxRes.data || []);
    setPreviousMonthTx(prevTxRes.data || []);
    setBudgets(budgetRes.data || []);
    setRecurringExpenses(recurringRes.data || []);
    setLoading(false);
  }, [user, supabase]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleTransactionAdded = () => {
    fetchData();
  };

  if (loading) {
    return (
      <div className="space-y-6 animate-fade-in">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          <Skeleton className="h-44 lg:col-span-2" />
          <Skeleton className="h-44" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Skeleton className="h-28" />
          <Skeleton className="h-28" />
          <Skeleton className="h-28" />
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <Skeleton className="h-72" />
          <Skeleton className="h-72" />
        </div>
      </div>
    );
  }

  // Calculate all derived data
  const balance = calculateBalance(allTransactions);
  const currentStats = calculateMonthlyStats(currentMonthTx);
  const previousStats = calculateMonthlyStats(previousMonthTx);
  const categorySpending = calculateCategorySpending(currentMonthTx);
  const dailySpending = calculateDailySpending(currentMonthTx);

  const upcomingRecurringTotal = recurringExpenses.reduce((sum, r) => sum + r.amount, 0);
  const safeToSpend = calculateSafeToSpend(
    balance,
    getDaysRemainingInMonth(),
    upcomingRecurringTotal,
    profile?.monthly_budget || 0
  );

  const daysPassed = new Date().getDate();
  const totalDays = getDaysInMonth();
  const predictedExpenses = predictMonthlyExpenses(currentStats.totalExpenses, daysPassed, totalDays);

  const overBudgetCount = budgets.filter(b => {
    const spent = categorySpending.find(c => c.category === b.category)?.amount || 0;
    return spent > b.amount;
  }).length;

  const budgetAdherence = budgets.length > 0
    ? ((budgets.length - overBudgetCount) / budgets.length) * 100
    : 100;

  const healthScore = calculateFinancialHealthScore({
    savingsRate: currentStats.savingsRate,
    budgetAdherence,
    spendingConsistency: 50,
    hasEmergencyFund: false,
    overBudgetCategories: overBudgetCount,
    totalCategories: budgets.length,
  });

  const insights = generateInsights({
    currentMonthTransactions: currentMonthTx,
    previousMonthTransactions: previousMonthTx,
    budgets,
    recurringExpenses,
    balance,
    lowBalanceThreshold: profile?.low_balance_threshold || 1000,
  });

  const currency = profile?.currency || 'INR';
  const hasTransactions = allTransactions.length > 0;

  if (!hasTransactions) {
    return (
      <>
        <EmptyDashboard
          onAddIncome={() => setShowAddIncome(true)}
          onAddExpense={() => setShowAddExpense(true)}
        />
        <AddExpenseModal
          open={showAddExpense}
          onClose={() => setShowAddExpense(false)}
          onSuccess={handleTransactionAdded}
          accountId={accounts[0]?.id}
        />
        <AddIncomeModal
          open={showAddIncome}
          onClose={() => setShowAddIncome(false)}
          onSuccess={handleTransactionAdded}
          accountId={accounts[0]?.id}
        />
      </>
    );
  }

  return (
    <div className="space-y-4 lg:space-y-6 animate-fade-in">
      {/* Row 1: Balance + Safe to Spend */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="lg:col-span-2">
          <BalanceCard
            balance={balance}
            totalIncome={currentStats.totalIncome}
            totalExpenses={currentStats.totalExpenses}
            previousExpenses={previousStats.totalExpenses}
            currency={currency}
            onAddExpense={() => setShowAddExpense(true)}
            onAddIncome={() => setShowAddIncome(true)}
          />
        </div>
        <SafeToSpendCard amount={safeToSpend} currency={currency} />
      </div>

      {/* Row 2: Monthly Overview */}
      <MonthlyOverview stats={currentStats} previousStats={previousStats} currency={currency} />

      {/* Row 3: Spending Chart + Category Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <SpendingChart dailyData={dailySpending} currency={currency} />
        <CategoryBreakdown categories={categorySpending} currency={currency} />
      </div>

      {/* Row 4: Financial Health + Budget Overview */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <FinancialHealthScore score={healthScore.score} factors={healthScore.factors} />
        <BudgetOverview budgets={budgets} categorySpending={categorySpending} currency={currency} />
      </div>

      {/* Row 5: Insights + Upcoming */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <SmartInsights insights={insights} />
        <UpcomingExpenses expenses={recurringExpenses} currency={currency} />
      </div>

      {/* Row 6: Recent Transactions */}
      <RecentTransactions transactions={currentMonthTx.slice(0, 8)} currency={currency} />

      {/* Modals */}
      <AddExpenseModal
        open={showAddExpense}
        onClose={() => setShowAddExpense(false)}
        onSuccess={handleTransactionAdded}
        accountId={accounts[0]?.id}
      />
      <AddIncomeModal
        open={showAddIncome}
        onClose={() => setShowAddIncome(false)}
        onSuccess={handleTransactionAdded}
        accountId={accounts[0]?.id}
      />
    </div>
  );
}
