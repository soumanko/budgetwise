'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { GeneratedInsight } from '@/lib/finance/insights';
import { TrendingUp, TrendingDown, AlertTriangle, Info, CheckCircle, Lightbulb } from 'lucide-react';

interface SmartInsightsProps {
  insights: GeneratedInsight[];
}

const severityConfig = {
  positive: { icon: CheckCircle, color: 'text-emerald-500', bg: 'bg-emerald-500/10', border: 'border-emerald-500/20' },
  warning: { icon: AlertTriangle, color: 'text-amber-500', bg: 'bg-amber-500/10', border: 'border-amber-500/20' },
  critical: { icon: AlertTriangle, color: 'text-rose-500', bg: 'bg-rose-500/10', border: 'border-rose-500/20' },
  info: { icon: Info, color: 'text-blue-500', bg: 'bg-blue-500/10', border: 'border-blue-500/20' },
};

export function SmartInsights({ insights }: SmartInsightsProps) {
  if (insights.length === 0) {
    return (
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Lightbulb className="w-4 h-4 text-amber-500" />
            <CardTitle>Smart Insights</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground text-center py-4">
            Add more transactions to get personalized insights
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <Lightbulb className="w-4 h-4 text-amber-500" />
          <CardTitle>Smart Insights</CardTitle>
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        {insights.slice(0, 5).map((insight, i) => {
          const config = severityConfig[insight.severity];
          const Icon = config.icon;

          return (
            <div
              key={i}
              className={`flex items-start gap-3 p-3 rounded-lg border ${config.border} ${config.bg}`}
            >
              <Icon className={`w-4 h-4 mt-0.5 shrink-0 ${config.color}`} />
              <div>
                <p className="text-sm font-medium">{insight.title}</p>
                <p className="text-xs text-muted-foreground mt-0.5">{insight.description}</p>
              </div>
            </div>
          );
        })}
      </CardContent>
    </Card>
  );
}
