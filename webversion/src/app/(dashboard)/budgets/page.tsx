'use client';

import { useState, useEffect, useCallback } from 'react';
import { createClient } from '@/lib/supabase/client';
import { useAuth } from '@/contexts/AuthContext';
import type { Budget, Transaction, CategorySpending } from '@/lib/types';
import { calculateCategorySpending, toMoney } from '@/lib/finance/calculations';
import { EXPENSE_CATEGORIES, getCategoryDef } from '@/lib/finance/categories';
import { formatCurrency, getMonthStart, getMonthEnd, getCurrentMonthDateStr } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Progress } from '@/components/ui/progress';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { Skeleton } from '@/components/ui/skeleton';
import { toast } from 'sonner';
import { PiggyBank, Plus, Trash2 } from 'lucide-react';

export default function BudgetsPage() {
  const { user, profile } = useAuth();
  const supabase = createClient();

  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [categorySpending, setCategorySpending] = useState<CategorySpending[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formCategory, setFormCategory] = useState('');
  const [formAmount, setFormAmount] = useState('');
  const [saving, setSaving] = useState(false);

  const currency = profile?.currency || 'INR';
  const currentMonth = getCurrentMonthDateStr();

  const fetchData = useCallback(async () => {
    if (!user) return;
    const monthStart = getMonthStart();
    const monthEnd = getMonthEnd();

    const [budgetRes, txRes] = await Promise.all([
      supabase.from('budgets').select('*').eq('user_id', user.id).eq('month', currentMonth),
      supabase.from('transactions').select('*').eq('user_id', user.id).eq('type', 'expense').gte('transaction_date', monthStart).lte('transaction_date', monthEnd),
    ]);

    setBudgets(budgetRes.data || []);
    setCategorySpending(calculateCategorySpending(txRes.data || []));
    setLoading(false);
  }, [user, supabase, currentMonth]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleSaveBudget = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    const amount = parseFloat(formAmount);
    if (isNaN(amount) || amount <= 0) {
      toast.error('Enter a valid amount');
      return;
    }
    if (!formCategory) {
      toast.error('Select a category');
      return;
    }

    setSaving(true);
    // Upsert: delete existing then insert
    await supabase.from('budgets').delete().eq('user_id', user.id).eq('category', formCategory).eq('month', currentMonth);
    const { error } = await supabase.from('budgets').insert({
      user_id: user.id,
      category: formCategory,
      amount,
      month: currentMonth,
    });

    if (error) {
      toast.error('Failed to save budget');
    } else {
      toast.success(`${formCategory} budget set to ${formatCurrency(amount, currency)}`);
      setFormCategory('');
      setFormAmount('');
      setShowForm(false);
      fetchData();
    }
    setSaving(false);
  };

  const handleDeleteBudget = async (id: string) => {
    const { error } = await supabase.from('budgets').delete().eq('id', id);
    if (error) {
      toast.error('Failed to delete budget');
    } else {
      toast.success('Budget removed');
      fetchData();
    }
  };

  const usedCategories = budgets.map(b => b.category);
  const availableCategories = EXPENSE_CATEGORIES.filter(c => !usedCategories.includes(c.name));

  if (loading) {
    return <div className="space-y-4"><Skeleton className="h-10 w-48" /><Skeleton className="h-64" /></div>;
  }

  return (
    <div className="space-y-4 lg:space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold">Budgets</h1>
          <p className="text-sm text-muted-foreground">Manage your monthly spending limits</p>
        </div>
        <Button onClick={() => setShowForm(true)} size="sm">
          <Plus className="w-4 h-4" /> Add Budget
        </Button>
      </div>

      {budgets.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-16 text-center">
            <PiggyBank className="w-12 h-12 text-muted-foreground mb-3" />
            <h3 className="font-semibold mb-1">No budgets set</h3>
            <p className="text-sm text-muted-foreground mb-4 max-w-xs">
              Set category budgets to track your spending limits and get warnings when you&apos;re close to exceeding them.
            </p>
            <Button onClick={() => setShowForm(true)}>
              <Plus className="w-4 h-4" /> Create Your First Budget
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {budgets.map((budget) => {
            const spent = categorySpending.find(c => c.category === budget.category)?.amount || 0;
            const utilization = budget.amount > 0 ? (spent / budget.amount) * 100 : 0;
            const isOver = spent > budget.amount;
            const remaining = toMoney(budget.amount - spent);
            const def = getCategoryDef(budget.category);
            const Icon = def.icon;

            return (
              <Card key={budget.id} className={`${isOver ? 'border-rose-500/30' : utilization >= 80 ? 'border-amber-500/30' : ''}`}>
                <CardContent className="p-5">
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2.5">
                      <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${def.bgColor}`}>
                        <Icon className={`w-4 h-4 ${def.color}`} />
                      </div>
                      <span className="font-medium">{budget.category}</span>
                    </div>
                    <button
                      onClick={() => handleDeleteBudget(budget.id)}
                      className="p-1 rounded text-muted-foreground hover:text-destructive cursor-pointer"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  <div className="space-y-2">
                    <div className="flex justify-between text-sm">
                      <span className={isOver ? 'text-rose-500 font-semibold' : 'font-semibold'}>
                        {formatCurrency(spent, currency)}
                      </span>
                      <span className="text-muted-foreground">/ {formatCurrency(budget.amount, currency)}</span>
                    </div>
                    <Progress
                      value={Math.min(utilization, 100)}
                      indicatorClassName={isOver ? 'bg-rose-500' : utilization >= 80 ? 'bg-amber-500' : 'bg-emerald-500'}
                    />
                    <p className={`text-xs ${isOver ? 'text-rose-500' : 'text-muted-foreground'}`}>
                      {isOver
                        ? `Over by ${formatCurrency(Math.abs(remaining), currency)}`
                        : `${formatCurrency(remaining, currency)} remaining`}
                    </p>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* Add Budget Dialog */}
      <Dialog open={showForm} onOpenChange={setShowForm}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Add Category Budget</DialogTitle>
            <DialogDescription>Set a monthly spending limit for a category</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleSaveBudget} className="space-y-4">
            <div className="space-y-2">
              <Label>Category</Label>
              <Select value={formCategory} onValueChange={setFormCategory}>
                <SelectTrigger><SelectValue placeholder="Select category" /></SelectTrigger>
                <SelectContent>
                  {availableCategories.map((cat) => (
                    <SelectItem key={cat.name} value={cat.name}>{cat.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="budget-amount">Monthly limit</Label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">₹</span>
                <Input id="budget-amount" type="number" placeholder="0" value={formAmount} onChange={(e) => setFormAmount(e.target.value)} className="pl-7" min="1" required />
              </div>
            </div>
            <Button type="submit" className="w-full" disabled={saving}>
              {saving ? 'Saving...' : 'Save Budget'}
            </Button>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
