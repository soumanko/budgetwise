'use client';

import { useState } from 'react';
import { createClient } from '@/lib/supabase/client';
import { useAuth } from '@/contexts/AuthContext';
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { INCOME_CATEGORIES } from '@/lib/finance/categories';
import { toISODateString } from '@/lib/utils';
import { toast } from 'sonner';
import { Plus } from 'lucide-react';

interface AddIncomeModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
  accountId?: string;
}

export function AddIncomeModal({ open, onClose, onSuccess, accountId }: AddIncomeModalProps) {
  const { user } = useAuth();
  const supabase = createClient();
  const [loading, setLoading] = useState(false);

  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('');
  const [description, setDescription] = useState('');
  const [date, setDate] = useState(toISODateString());
  const [notes, setNotes] = useState('');

  const resetForm = () => {
    setAmount('');
    setCategory('');
    setDescription('');
    setDate(toISODateString());
    setNotes('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    const parsedAmount = parseFloat(amount);
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      toast.error('Please enter a valid amount greater than 0');
      return;
    }
    if (parsedAmount > 10000000) {
      toast.error('Amount exceeds maximum limit');
      return;
    }
    if (!category) {
      toast.error('Please select a category');
      return;
    }
    if (!accountId) {
      toast.error('No account selected. Please create an account first.');
      return;
    }

    setLoading(true);

    const { error } = await supabase.from('transactions').insert({
      user_id: user.id,
      account_id: accountId,
      type: 'income',
      amount: parsedAmount,
      category,
      description: description || null,
      transaction_date: date,
      notes: notes || null,
    });

    if (error) {
      toast.error('Failed to add income. Please try again.');
      setLoading(false);
      return;
    }

    toast.success(`₹${parsedAmount.toLocaleString('en-IN')} added to your balance`);
    resetForm();
    setLoading(false);
    onClose();
    onSuccess();
  };

  return (
    <Dialog open={open} onOpenChange={(isOpen) => { if (!isOpen) onClose(); }}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-emerald-500/10 flex items-center justify-center">
              <Plus className="w-4 h-4 text-emerald-500" />
            </div>
            Add Money
          </DialogTitle>
          <DialogDescription>Record money received</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Amount */}
          <div className="space-y-2">
            <Label htmlFor="income-amount">Amount *</Label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-lg font-semibold text-muted-foreground">₹</span>
              <Input
                id="income-amount"
                type="number"
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="pl-8 text-lg h-12 font-semibold"
                min="0.01"
                step="0.01"
                max="10000000"
                required
                autoFocus
              />
            </div>
          </div>

          {/* Source category */}
          <div className="space-y-2">
            <Label>Source *</Label>
            <Select value={category} onValueChange={setCategory}>
              <SelectTrigger>
                <SelectValue placeholder="Where is this from?" />
              </SelectTrigger>
              <SelectContent>
                {INCOME_CATEGORIES.map((cat) => (
                  <SelectItem key={cat.name} value={cat.name}>
                    {cat.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Description */}
          <div className="space-y-2">
            <Label htmlFor="income-description">Description</Label>
            <Input
              id="income-description"
              placeholder="e.g. August allowance"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          {/* Date */}
          <div className="space-y-2">
            <Label htmlFor="income-date">Date *</Label>
            <Input
              id="income-date"
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              required
            />
          </div>

          {/* Notes */}
          <div className="space-y-2">
            <Label htmlFor="income-notes">Notes</Label>
            <Input
              id="income-notes"
              placeholder="Additional notes (optional)"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>

          <Button type="submit" variant="income" className="w-full" size="lg" disabled={loading}>
            {loading ? 'Adding...' : 'Add Money'}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}
