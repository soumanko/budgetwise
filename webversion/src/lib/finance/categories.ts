import {
  Utensils, Car, ShoppingBag, Film, GraduationCap, Receipt, Heart,
  Apple, Home, CreditCard, User, MoreHorizontal, Briefcase, Gift,
  BookOpen, ArrowDownLeft, Wallet, type LucideIcon
} from 'lucide-react';

export interface CategoryDef {
  name: string;
  icon: LucideIcon;
  color: string;
  bgColor: string;
}

export const EXPENSE_CATEGORIES: CategoryDef[] = [
  { name: 'Food', icon: Utensils, color: 'text-orange-500', bgColor: 'bg-orange-500/10' },
  { name: 'Travel', icon: Car, color: 'text-blue-500', bgColor: 'bg-blue-500/10' },
  { name: 'Shopping', icon: ShoppingBag, color: 'text-pink-500', bgColor: 'bg-pink-500/10' },
  { name: 'Entertainment', icon: Film, color: 'text-purple-500', bgColor: 'bg-purple-500/10' },
  { name: 'Education', icon: GraduationCap, color: 'text-indigo-500', bgColor: 'bg-indigo-500/10' },
  { name: 'Bills', icon: Receipt, color: 'text-yellow-500', bgColor: 'bg-yellow-500/10' },
  { name: 'Health', icon: Heart, color: 'text-red-500', bgColor: 'bg-red-500/10' },
  { name: 'Groceries', icon: Apple, color: 'text-green-500', bgColor: 'bg-green-500/10' },
  { name: 'Rent', icon: Home, color: 'text-teal-500', bgColor: 'bg-teal-500/10' },
  { name: 'Subscriptions', icon: CreditCard, color: 'text-cyan-500', bgColor: 'bg-cyan-500/10' },
  { name: 'Personal', icon: User, color: 'text-slate-500', bgColor: 'bg-slate-500/10' },
  { name: 'Other', icon: MoreHorizontal, color: 'text-gray-500', bgColor: 'bg-gray-500/10' },
];

export const INCOME_CATEGORIES: CategoryDef[] = [
  { name: 'Money from Home', icon: Home, color: 'text-emerald-500', bgColor: 'bg-emerald-500/10' },
  { name: 'Salary', icon: Briefcase, color: 'text-green-500', bgColor: 'bg-green-500/10' },
  { name: 'Freelancing', icon: BookOpen, color: 'text-blue-500', bgColor: 'bg-blue-500/10' },
  { name: 'Scholarship', icon: GraduationCap, color: 'text-indigo-500', bgColor: 'bg-indigo-500/10' },
  { name: 'Refund', icon: ArrowDownLeft, color: 'text-cyan-500', bgColor: 'bg-cyan-500/10' },
  { name: 'Gift', icon: Gift, color: 'text-pink-500', bgColor: 'bg-pink-500/10' },
  { name: 'Other', icon: Wallet, color: 'text-gray-500', bgColor: 'bg-gray-500/10' },
];

export const ALL_CATEGORIES = [...EXPENSE_CATEGORIES, ...INCOME_CATEGORIES];

export const PAYMENT_METHODS = [
  'UPI',
  'Cash',
  'Debit Card',
  'Credit Card',
  'Bank Transfer',
  'Other',
] as const;

export function getCategoryDef(categoryName: string, type: 'income' | 'expense' = 'expense'): CategoryDef {
  const list = type === 'income' ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;
  return list.find(c => c.name === categoryName) || {
    name: categoryName,
    icon: MoreHorizontal,
    color: 'text-gray-500',
    bgColor: 'bg-gray-500/10',
  };
}
