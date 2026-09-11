'use client';

import Link from 'next/link';
import { Card, CardContent } from '@/components/ui/card';
import { FileText, ArrowRight } from 'lucide-react';

const reports = [
  {
    href: '/reports/statement',
    title: 'Financial Statement',
    description: 'Generate a detailed statement of your income and spending for any period. Includes opening/closing balance, category breakdown, transaction details, and downloadable PDF.',
    icon: FileText,
  },
];

export default function ReportsPage() {
  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h1 className="text-xl font-semibold">Reports</h1>
        <p className="text-sm text-muted-foreground">
          Generate and export financial reports from your BudgetWise data
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {reports.map((report) => {
          const Icon = report.icon;
          return (
            <Link key={report.href} href={report.href}>
              <Card className="group h-full hover:border-primary/50 hover:shadow-md transition-all duration-200 cursor-pointer">
                <CardContent className="p-6 flex flex-col h-full">
                  <div className="flex items-center gap-3 mb-3">
                    <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
                      <Icon className="w-5 h-5 text-primary" />
                    </div>
                    <h2 className="text-base font-semibold">{report.title}</h2>
                  </div>
                  <p className="text-sm text-muted-foreground flex-1">
                    {report.description}
                  </p>
                  <div className="flex items-center gap-1 text-sm font-medium text-primary mt-4 group-hover:gap-2 transition-all duration-200">
                    <span>Open</span>
                    <ArrowRight className="w-4 h-4" />
                  </div>
                </CardContent>
              </Card>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
