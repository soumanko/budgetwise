// ============================================
// TypeScript Type Definitions
// ============================================

export type TransactionType = 'income' | 'expense';
export type AccountType = 'Bank' | 'Cash' | 'Savings' | 'Wallet' | 'Other';
export type Frequency = 'weekly' | 'monthly' | 'yearly';
export type Severity = 'positive' | 'warning' | 'critical' | 'info';
export type PaymentMethod = 'UPI' | 'Cash' | 'Debit Card' | 'Credit Card' | 'Bank Transfer' | 'Other';

export interface Profile {
  id: string;
  user_id: string;
  full_name: string;
  avatar_url: string | null;
  currency: string;
  monthly_budget: number;
  low_balance_threshold: number;
  created_at: string;
  updated_at: string;
}

export interface Account {
  id: string;
  user_id: string;
  name: string;
  account_type: AccountType;
  opening_balance: number;
  created_at: string;
  updated_at: string;
}

export interface Transaction {
  id: string;
  user_id: string;
  account_id: string | null;
  type: TransactionType;
  amount: number;
  category: string;
  subcategory: string | null;
  description: string | null;
  merchant: string | null;
  payment_method: PaymentMethod | null;
  transaction_date: string;
  notes: string | null;
  created_at: string;
  updated_at: string;
}

export interface Budget {
  id: string;
  user_id: string;
  category: string;
  amount: number;
  month: string;
  created_at: string;
  updated_at: string;
}

export interface RecurringExpense {
  id: string;
  user_id: string;
  name: string;
  amount: number;
  category: string;
  frequency: Frequency;
  next_due_date: string;
  active: boolean;
  notes: string | null;
  created_at: string;
  updated_at: string;
}

export interface SavingsGoal {
  id: string;
  user_id: string;
  name: string;
  target_amount: number;
  current_amount: number;
  deadline: string | null;
  description: string | null;
  created_at: string;
  updated_at: string;
}

export interface FinancialInsight {
  id: string;
  user_id: string;
  insight_type: string;
  title: string;
  description: string;
  severity: Severity;
  data: Record<string, unknown> | null;
  created_at: string;
}

export interface TransactionFormData {
  type: TransactionType;
  amount: string;
  category: string;
  subcategory?: string;
  description?: string;
  merchant?: string;
  payment_method?: PaymentMethod;
  transaction_date: string;
  notes?: string;
  account_id?: string;
}

export interface BudgetFormData {
  category: string;
  amount: string;
  month: string;
}

export interface GoalFormData {
  name: string;
  target_amount: string;
  current_amount: string;
  deadline?: string;
  description?: string;
}

export interface RecurringExpenseFormData {
  name: string;
  amount: string;
  category: string;
  frequency: Frequency;
  next_due_date: string;
  notes?: string;
}

export interface MonthlyStats {
  totalIncome: number;
  totalExpenses: number;
  netSavings: number;
  savingsRate: number;
  transactionCount: number;
}

export interface CategorySpending {
  category: string;
  amount: number;
  percentage: number;
  count: number;
}

export interface DailySpending {
  date: string;
  amount: number;
  income: number;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}
