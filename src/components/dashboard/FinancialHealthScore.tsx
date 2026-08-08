'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { CheckCircle, AlertTriangle, XCircle } from 'lucide-react';

interface FinancialHealthScoreProps {
  score: number;
  factors: { label: string; status: 'good' | 'warning' | 'bad'; detail: string }[];
}

export function FinancialHealthScore({ score, factors }: FinancialHealthScoreProps) {
  const getScoreColor = () => {
    if (score >= 75) return 'text-emerald-500';
    if (score >= 50) return 'text-amber-500';
    return 'text-rose-500';
  };

  const getScoreLabel = () => {
    if (score >= 75) return 'Excellent';
    if (score >= 50) return 'Good';
    if (score >= 25) return 'Fair';
    return 'Needs attention';
  };

  const circumference = 2 * Math.PI * 40;
  const dashOffset = circumference - (score / 100) * circumference;

  return (
    <Card>
      <CardHeader>
        <CardTitle>Financial Health</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex items-center gap-6">
          {/* Score ring */}
          <div className="relative w-24 h-24 shrink-0">
            <svg className="w-24 h-24 -rotate-90" viewBox="0 0 96 96">
              <circle cx="48" cy="48" r="40" fill="none" stroke="hsl(var(--muted))" strokeWidth="6" />
              <circle
                cx="48" cy="48" r="40" fill="none"
                stroke={score >= 75 ? '#10b981' : score >= 50 ? '#f59e0b' : '#ef4444'}
                strokeWidth="6" strokeLinecap="round"
                strokeDasharray={circumference}
                strokeDashoffset={dashOffset}
                className="transition-all duration-1000 ease-out"
              />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <span className={`text-xl font-bold ${getScoreColor()}`}>{score}</span>
              <span className="text-[10px] text-muted-foreground">/ 100</span>
            </div>
          </div>

          {/* Factors */}
          <div className="flex-1 space-y-2">
            <p className={`text-sm font-semibold ${getScoreColor()}`}>{getScoreLabel()}</p>
            {factors.slice(0, 4).map((f, i) => {
              const statusIcon = f.status === 'good'
                ? <CheckCircle className="w-3.5 h-3.5 text-emerald-500" />
                : f.status === 'warning'
                ? <AlertTriangle className="w-3.5 h-3.5 text-amber-500" />
                : <XCircle className="w-3.5 h-3.5 text-rose-500" />;

              return (
                <div key={i} className="flex items-start gap-2">
                  <span className="mt-0.5 shrink-0">{statusIcon}</span>
                  <p className="text-xs text-muted-foreground">{f.detail}</p>
                </div>
              );
            })}
          </div>
        </div>
        <p className="text-[10px] text-muted-foreground mt-3 italic">
          This score is calculated from your recorded data and is not an official financial rating.
        </p>
      </CardContent>
    </Card>
  );
}
