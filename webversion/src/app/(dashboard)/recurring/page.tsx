'use client';

import { useState, useEffect, useCallback } from 'react';
import { createClient } from '@/lib/supabase/client';
import { useAuth } from '@/contexts/AuthContext';
import type { RecurringExpense } from '@/lib/types';
import { EXPENSE_CATEGORIES, getCategoryDef } from '@/lib/finance/categories';
import { formatCurrency, formatDate, toISODateString } from '@/lib/utils';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { Skeleton } from '@/components/ui/skeleton';
import { toast } from 'sonner';
import { CalendarClock, Plus, Trash2, Pause, Play } from 'lucide-react';

export default function RecurringPage() {
  const { user, profile } = useAuth();
  const supabase = createClient();

  const [expenses, setExpenses] = useState<RecurringExpense[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formName, setFormName] = useState('');
  const [formAmount, setFormAmount] = useState('');
  const [formCategory, setFormCategory] = useState('');
  const [formFrequency, setFormFrequency] = useState('monthly');
  const [formNextDue, setFormNextDue] = useState(toISODateString());
  const [formNotes, setFormNotes] = useState('');
  const [saving, setSaving] = useState(false);

  const currency = profile?.currency || 'INR';

  const fetchExpenses = useCallback(async () => {
    if (!user) return;
    const { data } = await supabase.from('recurring_expenses').select('*').eq('user_id', user.id).order('next_due_date', { ascending: true });
    setExpenses(data || []);
    setLoading(false);
  }, [user, supabase]);

  useEffect(() => { fetchExpenses(); }, [fetchExpenses]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    const amount = parseFloat(formAmount);
    if (isNaN(amount) || amount <= 0) { toast.error('Enter a valid amount'); return; }

    setSaving(true);
    const { error } = await supabase.from('recurring_expenses').insert({
      user_id: user.id,
      name: formName,
      amount,
      category: formCategory,
      frequency: formFrequency,
      next_due_date: formNextDue,
      notes: formNotes || null,
    });

    if (error) { toast.error('Failed to create recurring expense'); }
    else {
      toast.success(`"${formName}" added`);
      setFormName(''); setFormAmount(''); setFormCategory(''); setFormFrequency('monthly'); setFormNextDue(toISODateString()); setFormNotes('');
      setShowForm(false);
      fetchExpenses();
    }
    setSaving(false);
  };

  const toggleActive = async (exp: RecurringExpense) => {
    const { error } = await supabase.from('recurring_expenses').update({ active: !exp.active }).eq('id', exp.id);
    if (!error) {
      toast.success(exp.active ? 'Paused' : 'Resumed');
      fetchExpenses();
    }
  };

  const handleDelete = async (id: string) => {
    const { error } = await supabase.from('recurring_expenses').delete().eq('id', id);
    if (!error) { toast.success('Deleted'); fetchExpenses(); }
  };

  const totalMonthly = expenses.filter(e => e.active).reduce((sum, e) => {
    if (e.frequency === 'monthly') return sum + e.amount;
    if (e.frequency === 'weekly') return sum + e.amount * 4.33;
    if (e.frequency === 'yearly') return sum + e.amount / 12;
    return sum;
  }, 0);

  if (loading) return <div className="space-y-4"><Skeleton className="h-10 w-48" /><Skeleton className="h-64" /></div>;

  return (
    <div className="space-y-4 lg:space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold">Recurring Expenses</h1>
          <p className="text-sm text-muted-foreground">
            {expenses.filter(e => e.active).length} active · ~{formatCurrency(totalMonthly, currency)}/month
          </p>
        </div>
        <Button onClick={() => setShowForm(true)} size="sm"><Plus className="w-4 h-4" /> Add</Button>
      </div>

      {expenses.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-16 text-center">
            <CalendarClock className="w-12 h-12 text-muted-foreground mb-3" />
            <h3 className="font-semibold mb-1">No recurring expenses</h3>
            <p className="text-sm text-muted-foreground mb-4 max-w-xs">Add subscriptions and bills to track upcoming expenses.</p>
            <Button onClick={() => setShowForm(true)}><Plus className="w-4 h-4" /> Add Recurring Expense</Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {expenses.map((exp) => {
            const def = getCategoryDef(exp.category);
            const Icon = def.icon;
            const now = new Date();
            const dueDate = new Date(exp.next_due_date);
            const daysUntil = Math.ceil((dueDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));

            return (
              <Card key={exp.id} className={!exp.active ? 'opacity-60' : ''}>
                <CardContent className="p-5">
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-center gap-2.5">
                      <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${def.bgColor}`}>
                        <Icon className={`w-4 h-4 ${def.color}`} />
                      </div>
                      <div>
                        <h3 className="font-semibold text-sm">{exp.name}</h3>
                        <p className="text-xs text-muted-foreground">{exp.category}</p>
                      </div>
                    </div>
                    <div className="flex gap-1">
                      <button onClick={() => toggleActive(exp)} className="p-1 text-muted-foreground hover:text-foreground cursor-pointer" title={exp.active ? 'Pause' : 'Resume'}>
                        {exp.active ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
                      </button>
                      <button onClick={() => handleDelete(exp.id)} className="p-1 text-muted-foreground hover:text-destructive cursor-pointer">
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>

                  <p className="text-xl font-bold">{formatCurrency(exp.amount, currency)}</p>
                  <div className="flex items-center gap-2 mt-2">
                    <Badge variant="secondary" className="text-[10px]">{exp.frequency}</Badge>
                    {exp.active && (
                      <span className={`text-xs ${daysUntil <= 3 ? 'text-amber-500 font-medium' : 'text-muted-foreground'}`}>
                        {daysUntil < 0 ? 'Overdue' : daysUntil === 0 ? 'Due today' : daysUntil === 1 ? 'Due tomorrow' : `Due in ${daysUntil} days`}
                      </span>
                    )}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* Add Dialog */}
      <Dialog open={showForm} onOpenChange={setShowForm}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Add Recurring Expense</DialogTitle>
            <DialogDescription>Track a subscription or regular bill</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleCreate} className="space-y-4">
            <div className="space-y-2">
              <Label>Name *</Label>
              <Input placeholder="e.g. Netflix" value={formName} onChange={(e) => setFormName(e.target.value)} required />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label>Amount *</Label>
                <div className="relative">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">₹</span>
                  <Input type="number" placeholder="649" value={formAmount} onChange={(e) => setFormAmount(e.target.value)} className="pl-7" min="1" required />
                </div>
              </div>
              <div className="space-y-2">
                <Label>Frequency *</Label>
                <Select value={formFrequency} onValueChange={setFormFrequency}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="weekly">Weekly</SelectItem>
                    <SelectItem value="monthly">Monthly</SelectItem>
                    <SelectItem value="yearly">Yearly</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label>Category *</Label>
                <Select value={formCategory} onValueChange={setFormCategory}>
                  <SelectTrigger><SelectValue placeholder="Select" /></SelectTrigger>
                  <SelectContent>
                    {EXPENSE_CATEGORIES.map((cat) => (
                      <SelectItem key={cat.name} value={cat.name}>{cat.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>Next due date *</Label>
                <Input type="date" value={formNextDue} onChange={(e) => setFormNextDue(e.target.value)} required />
              </div>
            </div>
            <div className="space-y-2">
              <Label>Notes</Label>
              <Input placeholder="Optional notes" value={formNotes} onChange={(e) => setFormNotes(e.target.value)} />
            </div>
            <Button type="submit" className="w-full" disabled={saving}>{saving ? 'Adding...' : 'Add'}</Button>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
