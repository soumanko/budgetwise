'use client';

import { useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { createClient } from '@/lib/supabase/client';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { useTheme } from '@/components/ThemeProvider';
import { toast } from 'sonner';
import { Settings, User, Palette, Shield, Download, Trash2 } from 'lucide-react';

const CURRENCIES = [
  { code: 'INR', label: 'Indian Rupee (₹)' },
  { code: 'USD', label: 'US Dollar ($)' },
  { code: 'EUR', label: 'Euro (€)' },
  { code: 'GBP', label: 'British Pound (£)' },
];

export default function SettingsPage() {
  const { user, profile, updateProfile, signOut } = useAuth();
  const { theme, setTheme } = useTheme();
  const supabase = createClient();

  const [fullName, setFullName] = useState(profile?.full_name || '');
  const [currency, setCurrency] = useState(profile?.currency || 'INR');
  const [monthlyBudget, setMonthlyBudget] = useState(String(profile?.monthly_budget || ''));
  const [lowBalanceThreshold, setLowBalanceThreshold] = useState(String(profile?.low_balance_threshold || ''));
  const [saving, setSaving] = useState(false);
  const [showClearData, setShowClearData] = useState(false);
  const [clearing, setClearing] = useState(false);

  const handleSaveProfile = async () => {
    setSaving(true);
    const { error } = await updateProfile({
      full_name: fullName,
      currency,
      monthly_budget: parseFloat(monthlyBudget) || 0,
      low_balance_threshold: parseFloat(lowBalanceThreshold) || 1000,
    });

    if (error) {
      toast.error('Failed to update profile');
    } else {
      toast.success('Profile updated');
    }
    setSaving(false);
  };

  const handleExportCSV = async () => {
    if (!user) return;
    const { data } = await supabase
      .from('transactions')
      .select('*')
      .eq('user_id', user.id)
      .order('transaction_date', { ascending: false });

    if (!data || data.length === 0) {
      toast.error('No transactions to export');
      return;
    }

    const headers = ['Date', 'Type', 'Amount', 'Category', 'Description', 'Merchant', 'Payment Method', 'Notes'];
    const rows = data.map(tx => [
      tx.transaction_date,
      tx.type,
      tx.amount,
      tx.category,
      tx.description || '',
      tx.merchant || '',
      tx.payment_method || '',
      tx.notes || '',
    ]);

    const csv = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `budgetwise-transactions-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    toast.success('Transactions exported');
  };

  const handleClearData = async () => {
    if (!user) return;
    setClearing(true);

    await Promise.all([
      supabase.from('transactions').delete().eq('user_id', user.id),
      supabase.from('budgets').delete().eq('user_id', user.id),
      supabase.from('recurring_expenses').delete().eq('user_id', user.id),
      supabase.from('savings_goals').delete().eq('user_id', user.id),
      supabase.from('financial_insights').delete().eq('user_id', user.id),
    ]);

    toast.success('All financial data cleared');
    setClearing(false);
    setShowClearData(false);
  };

  return (
    <div className="space-y-4 lg:space-y-6 max-w-2xl animate-fade-in">
      <div>
        <h1 className="text-xl font-semibold">Settings</h1>
        <p className="text-sm text-muted-foreground">Manage your account and preferences</p>
      </div>

      {/* Profile */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-foreground text-base">
            <User className="w-4 h-4" /> Profile
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="settings-name">Full name</Label>
            <Input id="settings-name" value={fullName} onChange={(e) => setFullName(e.target.value)} />
          </div>
          <div className="space-y-2">
            <Label>Email</Label>
            <Input value={user?.email || ''} disabled className="opacity-60" />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Currency</Label>
              <Select value={currency} onValueChange={setCurrency}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  {CURRENCIES.map(c => (
                    <SelectItem key={c.code} value={c.code}>{c.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Monthly budget</Label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">₹</span>
                <Input type="number" value={monthlyBudget} onChange={(e) => setMonthlyBudget(e.target.value)} className="pl-7" placeholder="15000" />
              </div>
            </div>
          </div>
          <div className="space-y-2">
            <Label>Low balance warning threshold</Label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">₹</span>
              <Input type="number" value={lowBalanceThreshold} onChange={(e) => setLowBalanceThreshold(e.target.value)} className="pl-7" placeholder="1000" />
            </div>
          </div>
          <Button onClick={handleSaveProfile} disabled={saving}>
            {saving ? 'Saving...' : 'Save Changes'}
          </Button>
        </CardContent>
      </Card>

      {/* Appearance */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-foreground text-base">
            <Palette className="w-4 h-4" /> Appearance
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
            <Label>Theme</Label>
            <Select value={theme} onValueChange={(v) => setTheme(v as 'light' | 'dark' | 'system')}>
              <SelectTrigger className="w-48"><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="light">Light</SelectItem>
                <SelectItem value="dark">Dark</SelectItem>
                <SelectItem value="system">System</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* Data */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-foreground text-base">
            <Download className="w-4 h-4" /> Data
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <Button variant="outline" onClick={handleExportCSV}>
            <Download className="w-4 h-4" /> Export transactions as CSV
          </Button>
          <div>
            <Button variant="destructive" onClick={() => setShowClearData(true)}>
              <Trash2 className="w-4 h-4" /> Clear all financial data
            </Button>
          </div>
          <div>
            <Button variant="outline" onClick={signOut}>
              Sign out
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Clear data confirmation */}
      <Dialog open={showClearData} onOpenChange={setShowClearData}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Clear all financial data?</DialogTitle>
            <DialogDescription>
              This will permanently delete all your transactions, budgets, goals, recurring expenses, and insights. This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <div className="flex gap-2 justify-end">
            <Button variant="outline" onClick={() => setShowClearData(false)}>Cancel</Button>
            <Button variant="destructive" onClick={handleClearData} disabled={clearing}>
              {clearing ? 'Clearing...' : 'Clear Everything'}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
