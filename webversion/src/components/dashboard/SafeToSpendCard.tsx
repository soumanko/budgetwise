'use client';

import { Card, CardContent } from '@/components/ui/card';
import { formatCurrency } from '@/lib/utils';
import { Shield } from 'lucide-react';

interface SafeToSpendCardProps {
  amount: number;
  currency: string;
}

export function SafeToSpendCard({ amount, currency }: SafeToSpendCardProps) {
  const level = amount > 500 ? 'good' : amount > 100 ? 'moderate' : 'low';
  const colors = {
    good: 'from-emerald-500/10 to-emerald-500/5 border-emerald-500/20',
    moderate: 'from-amber-500/10 to-amber-500/5 border-amber-500/20',
    low: 'from-rose-500/10 to-rose-500/5 border-rose-500/20',
  };
  const iconColors = {
    good: 'text-emerald-500 bg-emerald-500/10',
    moderate: 'text-amber-500 bg-amber-500/10',
    low: 'text-rose-500 bg-rose-500/10',
  };

  return (
    <Card className={`relative overflow-hidden bg-gradient-to-br ${colors[level]}`}>
      <CardContent className="p-5 lg:p-6 flex flex-col justify-between h-full">
        <div className="flex items-center justify-between">
          <p className="text-sm font-medium text-muted-foreground">Safe to Spend Today</p>
          <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${iconColors[level]}`}>
            <Shield className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-3">
          <p className="text-2xl lg:text-3xl font-bold tracking-tight">
            {formatCurrency(amount, currency)}
          </p>
          <p className="text-xs text-muted-foreground mt-2 leading-relaxed">
            Based on your current spending rate and upcoming expenses. This is an estimate, not financial advice.
          </p>
        </div>
      </CardContent>
    </Card>
  );
}
