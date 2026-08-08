'use client';

import { useState, useEffect, useCallback } from 'react';
import { createClient } from '@/lib/supabase/client';
import { useAuth } from '@/contexts/AuthContext';
import type { Transaction } from '@/lib/types';
import { formatCurrency, formatDate } from '@/lib/utils';
import { getCategoryDef, EXPENSE_CATEGORIES, INCOME_CATEGORIES } from '@/lib/finance/categories';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { AddExpenseModal } from '@/components/transactions/AddExpenseModal';
import { AddIncomeModal } from '@/components/transactions/AddIncomeModal';
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription,
} from '@/components/ui/dialog';
import { toast } from 'sonner';
import { Plus, Minus, Search, Trash2, Edit, Filter, ArrowLeftRight } from 'lucide-react';

export default function TransactionsPage() {
  const { user, accounts } = useAuth();
  const supabase = createClient();

  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAddExpense, setShowAddExpense] = useState(false);
  const [showAddIncome, setShowAddIncome] = useState(false);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<string>('all');
  const [categoryFilter, setCategoryFilter] = useState<string>('all');
  const [deleteTarget, setDeleteTarget] = useState<Transaction | null>(null);
  const [deleting, setDeleting] = useState(false);

  const fetchTransactions = useCallback(async () => {
    if (!user) return;
    let query = supabase
      .from('transactions')
      .select('*')
      .eq('user_id', user.id)
      .order('transaction_date', { ascending: false })
      .order('created_at', { ascending: false })
      .limit(200);

    if (typeFilter !== 'all') {
      query = query.eq('type', typeFilter);
    }
    if (categoryFilter !== 'all') {
      query = query.eq('category', categoryFilter);
    }

    const { data } = await query;
    setTransactions(data || []);
    setLoading(false);
  }, [user, supabase, typeFilter, categoryFilter]);

  useEffect(() => {
    fetchTransactions();
  }, [fetchTransactions]);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    const { error } = await supabase.from('transactions').delete().eq('id', deleteTarget.id);
    if (error) {
      toast.error('Failed to delete transaction');
    } else {
      toast.success('Transaction deleted');
      fetchTransactions();
    }
    setDeleting(false);
    setDeleteTarget(null);
  };

  const filtered = transactions.filter(tx => {
    if (!search) return true;
    const q = search.toLowerCase();
    return (
      tx.description?.toLowerCase().includes(q) ||
      tx.merchant?.toLowerCase().includes(q) ||
      tx.category.toLowerCase().includes(q) ||
      tx.notes?.toLowerCase().includes(q)
    );
  });

  const allCategories = [...EXPENSE_CATEGORIES, ...INCOME_CATEGORIES];

  if (loading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-96" />
      </div>
    );
  }

  return (
    <div className="space-y-4 lg:space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold">Transactions</h1>
          <p className="text-sm text-muted-foreground">{transactions.length} total transactions</p>
        </div>
        <div className="flex gap-2">
          <Button onClick={() => setShowAddIncome(true)} variant="income" size="sm">
            <Plus className="w-4 h-4" /> Add Money
          </Button>
          <Button onClick={() => setShowAddExpense(true)} variant="expense" size="sm">
            <Minus className="w-4 h-4" /> Add Expense
          </Button>
        </div>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="p-3 lg:p-4">
          <div className="flex flex-col sm:flex-row gap-2">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input
                placeholder="Search transactions..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9"
              />
            </div>
            <Select value={typeFilter} onValueChange={setTypeFilter}>
              <SelectTrigger className="w-full sm:w-36">
                <SelectValue placeholder="Type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All types</SelectItem>
                <SelectItem value="income">Income</SelectItem>
                <SelectItem value="expense">Expense</SelectItem>
              </SelectContent>
            </Select>
            <Select value={categoryFilter} onValueChange={setCategoryFilter}>
              <SelectTrigger className="w-full sm:w-44">
                <SelectValue placeholder="Category" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All categories</SelectItem>
                {allCategories.map((cat) => (
                  <SelectItem key={cat.name} value={cat.name}>{cat.name}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* Transaction list */}
      <Card>
        <CardContent className="p-0">
          {filtered.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-center">
              <ArrowLeftRight className="w-10 h-10 text-muted-foreground mb-3" />
              <p className="text-sm text-muted-foreground">No transactions found</p>
            </div>
          ) : (
            <div className="divide-y divide-border">
              {filtered.map((tx) => {
                const def = getCategoryDef(tx.category, tx.type);
                const Icon = def.icon;
                const isIncome = tx.type === 'income';

                return (
                  <div key={tx.id} className="flex items-center gap-3 p-3 lg:p-4 hover:bg-accent/30 transition-colors group">
                    <div className={`w-10 h-10 rounded-lg flex items-center justify-center shrink-0 ${def.bgColor}`}>
                      <Icon className={`w-4 h-4 ${def.color}`} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium truncate">
                        {tx.description || tx.merchant || tx.category}
                      </p>
                      <div className="flex items-center gap-2 mt-0.5">
                        <Badge variant={isIncome ? 'income' : 'expense'} className="text-[10px] h-4">
                          {tx.type}
                        </Badge>
                        <span className="text-xs text-muted-foreground">{tx.category}</span>
                        <span className="text-xs text-muted-foreground hidden sm:inline">·</span>
                        <span className="text-xs text-muted-foreground hidden sm:inline">{formatDate(tx.transaction_date)}</span>
                        {tx.payment_method && (
                          <>
                            <span className="text-xs text-muted-foreground hidden md:inline">·</span>
                            <span className="text-xs text-muted-foreground hidden md:inline">{tx.payment_method}</span>
                          </>
                        )}
                      </div>
                    </div>
                    <span className={`text-sm font-semibold whitespace-nowrap ${isIncome ? 'text-emerald-600 dark:text-emerald-400' : ''}`}>
                      {isIncome ? '+' : '-'}{formatCurrency(tx.amount, 'INR')}
                    </span>
                    <button
                      onClick={() => setDeleteTarget(tx)}
                      className="p-1.5 rounded-md opacity-0 group-hover:opacity-100 text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-all cursor-pointer"
                      title="Delete transaction"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Delete confirmation */}
      <Dialog open={!!deleteTarget} onOpenChange={() => setDeleteTarget(null)}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Delete transaction?</DialogTitle>
            <DialogDescription>
              This will delete the {deleteTarget?.type} of {deleteTarget ? formatCurrency(deleteTarget.amount, 'INR') : ''} and update your balance. This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <div className="flex gap-2 justify-end">
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>Cancel</Button>
            <Button variant="destructive" onClick={handleDelete} disabled={deleting}>
              {deleting ? 'Deleting...' : 'Delete'}
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* Modals */}
      <AddExpenseModal open={showAddExpense} onClose={() => setShowAddExpense(false)} onSuccess={fetchTransactions} accountId={accounts[0]?.id} />
      <AddIncomeModal open={showAddIncome} onClose={() => setShowAddIncome(false)} onSuccess={fetchTransactions} accountId={accounts[0]?.id} />
    </div>
  );
}
