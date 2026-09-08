'use client';

import { useState, useEffect, useCallback } from 'react';
import { createClient } from '@/lib/supabase/client';
import { useAuth } from '@/contexts/AuthContext';
import type { SavingsGoal } from '@/lib/types';
import { formatCurrency, toISODateString } from '@/lib/utils';
import { toMoney } from '@/lib/finance/calculations';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Progress } from '@/components/ui/progress';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { Skeleton } from '@/components/ui/skeleton';
import { toast } from 'sonner';
import { Target, Plus, Trash2, TrendingUp } from 'lucide-react';

export default function GoalsPage() {
  const { user, profile } = useAuth();
  const supabase = createClient();

  const [goals, setGoals] = useState<SavingsGoal[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formName, setFormName] = useState('');
  const [formTarget, setFormTarget] = useState('');
  const [formCurrent, setFormCurrent] = useState('0');
  const [formDeadline, setFormDeadline] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [saving, setSaving] = useState(false);

  // For adding money to goals
  const [addGoalId, setAddGoalId] = useState<string | null>(null);
  const [addAmount, setAddAmount] = useState('');

  const currency = profile?.currency || 'INR';

  const fetchGoals = useCallback(async () => {
    if (!user) return;
    const { data } = await supabase.from('savings_goals').select('*').eq('user_id', user.id).order('created_at', { ascending: false });
    setGoals(data || []);
    setLoading(false);
  }, [user, supabase]);

  useEffect(() => { fetchGoals(); }, [fetchGoals]);

  const handleCreateGoal = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    const target = parseFloat(formTarget);
    const current = parseFloat(formCurrent) || 0;
    if (isNaN(target) || target <= 0) { toast.error('Enter a valid target amount'); return; }

    setSaving(true);
    const { error } = await supabase.from('savings_goals').insert({
      user_id: user.id,
      name: formName,
      target_amount: target,
      current_amount: current,
      deadline: formDeadline || null,
      description: formDescription || null,
    });

    if (error) { toast.error('Failed to create goal'); }
    else {
      toast.success(`Goal "${formName}" created`);
      setFormName(''); setFormTarget(''); setFormCurrent('0'); setFormDeadline(''); setFormDescription('');
      setShowForm(false);
      fetchGoals();
    }
    setSaving(false);
  };

  const handleAddToGoal = async () => {
    if (!addGoalId) return;
    const amount = parseFloat(addAmount);
    if (isNaN(amount) || amount <= 0) { toast.error('Enter a valid amount'); return; }

    const goal = goals.find(g => g.id === addGoalId);
    if (!goal) return;

    const newCurrent = toMoney(goal.current_amount + amount);
    const { error } = await supabase.from('savings_goals').update({ current_amount: newCurrent }).eq('id', addGoalId);

    if (error) { toast.error('Failed to update goal'); }
    else {
      toast.success(`${formatCurrency(amount, currency)} added to "${goal.name}"`);
      setAddGoalId(null);
      setAddAmount('');
      fetchGoals();
    }
  };

  const handleDeleteGoal = async (id: string) => {
    const { error } = await supabase.from('savings_goals').delete().eq('id', id);
    if (!error) { toast.success('Goal deleted'); fetchGoals(); }
  };

  if (loading) return <div className="space-y-4"><Skeleton className="h-10 w-48" /><Skeleton className="h-64" /></div>;

  return (
    <div className="space-y-4 lg:space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold">Savings Goals</h1>
          <p className="text-sm text-muted-foreground">Track progress toward your financial goals</p>
        </div>
        <Button onClick={() => setShowForm(true)} size="sm"><Plus className="w-4 h-4" /> New Goal</Button>
      </div>

      {goals.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-16 text-center">
            <Target className="w-12 h-12 text-muted-foreground mb-3" />
            <h3 className="font-semibold mb-1">No savings goals yet</h3>
            <p className="text-sm text-muted-foreground mb-4 max-w-xs">Create goals like &ldquo;New Laptop&rdquo; or &ldquo;Emergency Fund&rdquo; to stay motivated.</p>
            <Button onClick={() => setShowForm(true)}><Plus className="w-4 h-4" /> Create Your First Goal</Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {goals.map((goal) => {
            const progress = goal.target_amount > 0 ? (goal.current_amount / goal.target_amount) * 100 : 0;
            const remaining = toMoney(goal.target_amount - goal.current_amount);
            const isComplete = goal.current_amount >= goal.target_amount;

            let monthsLeft = null;
            if (goal.deadline) {
              const deadlineDate = new Date(goal.deadline);
              const now = new Date();
              monthsLeft = Math.max(0, (deadlineDate.getFullYear() - now.getFullYear()) * 12 + (deadlineDate.getMonth() - now.getMonth()));
            }
            const requiredMonthly = monthsLeft && monthsLeft > 0 ? toMoney(remaining / monthsLeft) : null;

            return (
              <Card key={goal.id} className={isComplete ? 'border-emerald-500/30' : ''}>
                <CardContent className="p-5">
                  <div className="flex items-start justify-between mb-3">
                    <div>
                      <h3 className="font-semibold">{goal.name}</h3>
                      {goal.description && <p className="text-xs text-muted-foreground mt-0.5">{goal.description}</p>}
                    </div>
                    <button onClick={() => handleDeleteGoal(goal.id)} className="p-1 text-muted-foreground hover:text-destructive cursor-pointer">
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  <div className="space-y-2">
                    <div className="flex justify-between text-sm">
                      <span className="font-semibold">{formatCurrency(goal.current_amount, currency)}</span>
                      <span className="text-muted-foreground">/ {formatCurrency(goal.target_amount, currency)}</span>
                    </div>
                    <Progress
                      value={Math.min(progress, 100)}
                      indicatorClassName={isComplete ? 'bg-emerald-500' : 'bg-primary'}
                    />
                    <div className="flex justify-between text-xs text-muted-foreground">
                      <span>{progress.toFixed(1)}% complete</span>
                      {!isComplete && <span>{formatCurrency(remaining, currency)} to go</span>}
                    </div>
                  </div>

                  {requiredMonthly && !isComplete && (
                    <p className="text-xs text-muted-foreground mt-2">
                      Save {formatCurrency(requiredMonthly, currency)}/month to reach by deadline
                    </p>
                  )}

                  {isComplete ? (
                    <div className="mt-3 p-2 rounded-lg bg-emerald-500/10 text-center">
                      <p className="text-xs font-medium text-emerald-600 dark:text-emerald-400">🎉 Goal reached!</p>
                    </div>
                  ) : (
                    <Button size="sm" variant="outline" className="w-full mt-3" onClick={() => setAddGoalId(goal.id)}>
                      <Plus className="w-3 h-3" /> Add Savings
                    </Button>
                  )}
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* Create Goal Dialog */}
      <Dialog open={showForm} onOpenChange={setShowForm}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Create Savings Goal</DialogTitle>
            <DialogDescription>Set a target and track your progress</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleCreateGoal} className="space-y-4">
            <div className="space-y-2">
              <Label>Goal name *</Label>
              <Input placeholder="e.g. New Laptop" value={formName} onChange={(e) => setFormName(e.target.value)} required />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label>Target amount *</Label>
                <div className="relative">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">₹</span>
                  <Input type="number" placeholder="80000" value={formTarget} onChange={(e) => setFormTarget(e.target.value)} className="pl-7" min="1" required />
                </div>
              </div>
              <div className="space-y-2">
                <Label>Saved so far</Label>
                <div className="relative">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">₹</span>
                  <Input type="number" placeholder="0" value={formCurrent} onChange={(e) => setFormCurrent(e.target.value)} className="pl-7" min="0" />
                </div>
              </div>
            </div>
            <div className="space-y-2">
              <Label>Deadline</Label>
              <Input type="date" value={formDeadline} onChange={(e) => setFormDeadline(e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label>Description</Label>
              <Input placeholder="Optional description" value={formDescription} onChange={(e) => setFormDescription(e.target.value)} />
            </div>
            <Button type="submit" className="w-full" disabled={saving}>{saving ? 'Creating...' : 'Create Goal'}</Button>
          </form>
        </DialogContent>
      </Dialog>

      {/* Add Savings Dialog */}
      <Dialog open={!!addGoalId} onOpenChange={() => setAddGoalId(null)}>
        <DialogContent className="sm:max-w-xs">
          <DialogHeader>
            <DialogTitle>Add Savings</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">₹</span>
              <Input type="number" placeholder="0" value={addAmount} onChange={(e) => setAddAmount(e.target.value)} className="pl-7" min="1" autoFocus />
            </div>
            <Button className="w-full" onClick={handleAddToGoal}>Add</Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
