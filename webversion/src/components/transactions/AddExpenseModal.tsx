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
import { EXPENSE_CATEGORIES, PAYMENT_METHODS } from '@/lib/finance/categories';
import { toISODateString } from '@/lib/utils';
import { toast } from 'sonner';
import { Minus } from 'lucide-react';

interface AddExpenseModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
  accountId?: string;
}

export function AddExpenseModal({ open, onClose, onSuccess, accountId }: AddExpenseModalProps) {
  const { user } = useAuth();
  const supabase = createClient();
  const [loading, setLoading] = useState(false);

  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('');
  const [description, setDescription] = useState('');
  const [merchant, setMerchant] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('');
  const [date, setDate] = useState(toISODateString());
  const [notes, setNotes] = useState('');

  const resetForm = () => {
    setAmount('');
    setCategory('');
    setDescription('');
    setMerchant('');
    setPaymentMethod('');
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
      type: 'expense',
      amount: parsedAmount,
      category,
      description: description || null,
      merchant: merchant || null,
      payment_method: paymentMethod || null,
      transaction_date: date,
      notes: notes || null,
    });

    if (error) {
      toast.error('Failed to add expense. Please try again.');
      setLoading(false);
      return;
    }

    toast.success(`₹${parsedAmount.toLocaleString('en-IN')} expense added`);
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
            <div className="w-8 h-8 rounded-lg bg-rose-500/10 flex items-center justify-center">
              <Minus className="w-4 h-4 text-rose-500" />
            </div>
            Add Expense
          </DialogTitle>
          <DialogDescription>Record a new expense transaction</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Amount - prominent */}
          <div className="space-y-2">
            <Label htmlFor="expense-amount">Amount *</Label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-lg font-semibold text-muted-foreground">₹</span>
              <Input
                id="expense-amount"
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

          {/* Category */}
          <div className="space-y-2">
            <Label>Category *</Label>
            <Select value={category} onValueChange={setCategory}>
              <SelectTrigger>
                <SelectValue placeholder="Select category" />
              </SelectTrigger>
              <SelectContent>
                {EXPENSE_CATEGORIES.map((cat) => (
                  <SelectItem key={cat.name} value={cat.name}>
                    {cat.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Description + Merchant - two columns */}
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor="expense-description">Description</Label>
              <Input
                id="expense-description"
                placeholder="e.g. Lunch"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="expense-merchant">Merchant</Label>
              <Input
                id="expense-merchant"
                placeholder="e.g. Zomato"
                value={merchant}
                onChange={(e) => setMerchant(e.target.value)}
              />
            </div>
          </div>

          {/* Payment Method + Date - two columns */}
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label>Payment Method</Label>
              <Select value={paymentMethod} onValueChange={setPaymentMethod}>
                <SelectTrigger>
                  <SelectValue placeholder="Select" />
                </SelectTrigger>
                <SelectContent>
                  {PAYMENT_METHODS.map((pm) => (
                    <SelectItem key={pm} value={pm}>{pm}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="expense-date">Date *</Label>
              <Input
                id="expense-date"
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                required
              />
            </div>
          </div>

          {/* Notes */}
          <div className="space-y-2">
            <Label htmlFor="expense-notes">Notes</Label>
            <Input
              id="expense-notes"
              placeholder="Additional notes (optional)"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>

          <Button type="submit" variant="expense" className="w-full" size="lg" disabled={loading}>
            {loading ? 'Adding...' : 'Add Expense'}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}
