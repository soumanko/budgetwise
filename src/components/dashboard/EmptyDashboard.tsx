'use client';

import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Wallet, Plus, Minus, BarChart3, TrendingUp } from 'lucide-react';

interface EmptyDashboardProps {
  onAddIncome: () => void;
  onAddExpense: () => void;
}

export function EmptyDashboard({ onAddIncome, onAddExpense }: EmptyDashboardProps) {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] animate-fade-in">
      <div className="max-w-md text-center">
        {/* Illustration */}
        <div className="relative mx-auto w-32 h-32 mb-8">
          <div className="absolute inset-0 rounded-full bg-primary/5 animate-pulse" />
          <div className="absolute inset-4 rounded-full bg-primary/10 flex items-center justify-center">
            <Wallet className="w-12 h-12 text-primary" />
          </div>
          <div className="absolute -top-1 -right-1 w-8 h-8 rounded-full bg-emerald-500/10 flex items-center justify-center">
            <TrendingUp className="w-4 h-4 text-emerald-500" />
          </div>
          <div className="absolute -bottom-1 -left-1 w-8 h-8 rounded-full bg-blue-500/10 flex items-center justify-center">
            <BarChart3 className="w-4 h-4 text-blue-500" />
          </div>
        </div>

        <h2 className="text-xl font-semibold mb-2">Your financial picture starts here</h2>
        <p className="text-muted-foreground text-sm mb-8 leading-relaxed">
          Add your first income or expense to start tracking. Your dashboard will show balance, spending trends, budgets, and smart insights as you record transactions.
        </p>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
          <Button onClick={onAddIncome} variant="income" size="lg" className="w-full sm:w-auto">
            <Plus className="w-4 h-4" />
            Add Money
          </Button>
          <Button onClick={onAddExpense} variant="expense" size="lg" className="w-full sm:w-auto">
            <Minus className="w-4 h-4" />
            Add Expense
          </Button>
        </div>

        <div className="mt-8 grid grid-cols-3 gap-4">
          {['Track expenses', 'Set budgets', 'AI insights'].map((feature) => (
            <div key={feature} className="text-center">
              <p className="text-xs text-muted-foreground">{feature}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
