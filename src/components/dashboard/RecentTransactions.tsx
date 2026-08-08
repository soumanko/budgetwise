'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { formatCurrency, formatDate } from '@/lib/utils';
import { getCategoryDef } from '@/lib/finance/categories';
import type { Transaction } from '@/lib/types';
import Link from 'next/link';
import { ArrowRight } from 'lucide-react';

interface RecentTransactionsProps {
  transactions: Transaction[];
  currency: string;
}

export function RecentTransactions({ transactions, currency }: RecentTransactionsProps) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>Recent Transactions</CardTitle>
        <Link href="/transactions" className="text-xs text-primary hover:underline flex items-center gap-1">
          View all <ArrowRight className="w-3 h-3" />
        </Link>
      </CardHeader>
      <CardContent>
        {transactions.length === 0 ? (
          <p className="text-sm text-muted-foreground text-center py-4">No transactions yet</p>
        ) : (
          <div className="space-y-2">
            {transactions.slice(0, 8).map((tx) => {
              const def = getCategoryDef(tx.category, tx.type);
              const Icon = def.icon;
              const isIncome = tx.type === 'income';

              return (
                <div key={tx.id} className="flex items-center gap-3 p-2 rounded-lg hover:bg-accent/50 transition-colors">
                  <div className={`w-9 h-9 rounded-lg flex items-center justify-center shrink-0 ${def.bgColor}`}>
                    <Icon className={`w-4 h-4 ${def.color}`} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium truncate">
                      {tx.description || tx.merchant || tx.category}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {tx.category} · {formatDate(tx.transaction_date)}
                    </p>
                  </div>
                  <span className={`text-sm font-semibold whitespace-nowrap ${isIncome ? 'text-emerald-600 dark:text-emerald-400' : 'text-foreground'}`}>
                    {isIncome ? '+' : '-'}{formatCurrency(tx.amount, currency)}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
