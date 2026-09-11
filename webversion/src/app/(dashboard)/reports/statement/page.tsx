'use client';

import { useState, useCallback } from 'react';
import { createClient } from '@/lib/supabase/client';
import { useAuth } from '@/contexts/AuthContext';
import { buildFinancialStatement, type FinancialStatementData } from '@/lib/finance/statement';
import { generateFinancialStatementPdf, getStatementFilename } from '@/lib/finance/pdf';
import { FinancialStatementDocument } from '@/components/reports/FinancialStatementDocument';
import { getMonthStart, toISODateString } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { FileText, Download, Share2, Loader2, ArrowLeft, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';
import Link from 'next/link';

export default function FinancialStatementPage() {
  const { user, profile } = useAuth();
  const supabase = createClient();
  const currency = profile?.currency || 'INR';

  const [startDate, setStartDate] = useState(getMonthStart());
  const [endDate, setEndDate] = useState(toISODateString());
  const [statementData, setStatementData] = useState<FinancialStatementData | null>(null);
  const [loading, setLoading] = useState(false);
  const [hasGenerated, setHasGenerated] = useState(false);
  const [dateError, setDateError] = useState<string | null>(null);

  // ---- Validation ----
  const validateDates = useCallback((s: string, e: string): boolean => {
    if (!s || !e) {
      setDateError('Please select both start and end dates.');
      return false;
    }
    if (s > e) {
      setDateError('Start date must be on or before end date.');
      return false;
    }
    setDateError(null);
    return true;
  }, []);

  // ---- Generate ----
  const generateStatement = useCallback(async () => {
    if (!user) return;
    if (!validateDates(startDate, endDate)) return;

    setLoading(true);
    setHasGenerated(true);
    setStatementData(null);

    try {
      // Query 1: transactions within the selected period
      const { data: periodTx, error: periodErr } = await supabase
        .from('transactions')
        .select('*')
        .eq('user_id', user.id)
        .gte('transaction_date', startDate)
        .lte('transaction_date', endDate)
        .order('transaction_date', { ascending: true });

      if (periodErr) throw periodErr;

      // Query 2: transactions BEFORE the start date (for opening balance)
      // Only fetch the fields calculateBalance() actually reads: type, amount
      const { data: priorTx, error: priorErr } = await supabase
        .from('transactions')
        .select('id, type, amount')
        .eq('user_id', user.id)
        .lt('transaction_date', startDate);

      if (priorErr) throw priorErr;

      // calculateBalance only accesses t.type and t.amount
      let openingBalanceCents = 0;
      for (const t of (priorTx || [])) {
        const amountCents = Math.round(t.amount * 100);
        if (t.type === 'income') {
          openingBalanceCents += amountCents;
        } else {
          openingBalanceCents -= amountCents;
        }
      }
      const openingBalance = openingBalanceCents / 100;

      const statement = buildFinancialStatement(
        periodTx || [],
        openingBalance,
        startDate,
        endDate,
        currency,
      );

      setStatementData(statement);
    } catch (err) {
      console.error('Statement generation failed:', err);
      toast.error('Failed to load transactions. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [user, supabase, startDate, endDate, currency, validateDates]);

  // ---- PDF download ----
  const handleDownloadPDF = useCallback(() => {
    if (!statementData) return;
    try {
      const blob = generateFinancialStatementPdf(statementData);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = getStatementFilename(statementData.startDate, statementData.endDate);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      toast.success('PDF downloaded successfully');
    } catch (err) {
      console.error('PDF generation failed:', err);
      toast.error('Failed to generate PDF. Please try again.');
    }
  }, [statementData]);

  // ---- Share ----
  const handleShare = useCallback(async () => {
    if (!statementData) return;
    try {
      const blob = generateFinancialStatementPdf(statementData);
      const filename = getStatementFilename(statementData.startDate, statementData.endDate);
      const file = new File([blob], filename, { type: 'application/pdf' });

      if (typeof navigator.canShare === 'function' && navigator.canShare({ files: [file] })) {
        await navigator.share({
          files: [file],
          title: 'BudgetWise Financial Statement',
          text: `Financial statement for ${statementData.startDate} to ${statementData.endDate}`,
        });
      } else {
        toast.error('Sharing is not supported on this device. Use Download instead.');
      }
    } catch (err: any) {
      if (err?.name !== 'AbortError') {
        console.error('Share failed:', err);
        toast.error('Failed to share. Please download the PDF instead.');
      }
    }
  }, [statementData]);

  return (
    <div className="space-y-6 animate-fade-in">
      {/* ---- Page header ---- */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="flex items-center gap-3">
          <Link href="/reports" className="text-muted-foreground hover:text-foreground transition-colors">
            <ArrowLeft className="w-5 h-5" />
          </Link>
          <div>
            <h1 className="text-xl font-semibold">Financial Statement</h1>
            <p className="text-sm text-muted-foreground">
              Generate a detailed statement of your income and spending for any period
            </p>
          </div>
        </div>

        {statementData && statementData.transactions.length > 0 && (
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={handleShare}>
              <Share2 className="w-4 h-4 mr-2" />
              Share
            </Button>
            <Button size="sm" onClick={handleDownloadPDF}>
              <Download className="w-4 h-4 mr-2" />
              Download PDF
            </Button>
          </div>
        )}
      </div>

      {/* ---- Date range picker ---- */}
      <div className="bg-card border border-border rounded-xl p-4 sm:p-6">
        <div className="flex flex-col sm:flex-row gap-4 items-end">
          <div className="w-full sm:w-auto flex-1 space-y-1.5">
            <label htmlFor="stmt-start" className="text-xs font-medium text-muted-foreground">Start Date</label>
            <input
              id="stmt-start"
              type="date"
              value={startDate}
              max={endDate}
              onChange={(e) => {
                setStartDate(e.target.value);
                setDateError(null);
              }}
              className="flex h-10 w-full rounded-lg border border-input bg-transparent px-3 py-2 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            />
          </div>
          <div className="w-full sm:w-auto flex-1 space-y-1.5">
            <label htmlFor="stmt-end" className="text-xs font-medium text-muted-foreground">End Date</label>
            <input
              id="stmt-end"
              type="date"
              value={endDate}
              min={startDate}
              onChange={(e) => {
                setEndDate(e.target.value);
                setDateError(null);
              }}
              className="flex h-10 w-full rounded-lg border border-input bg-transparent px-3 py-2 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            />
          </div>
          <Button
            onClick={generateStatement}
            disabled={loading}
            className="w-full sm:w-auto h-10"
          >
            {loading
              ? <Loader2 className="w-4 h-4 mr-2 animate-spin" />
              : <FileText className="w-4 h-4 mr-2" />
            }
            Generate Statement
          </Button>
        </div>
        {dateError && (
          <div className="flex items-center gap-2 mt-3 text-sm text-destructive">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{dateError}</span>
          </div>
        )}
      </div>

      {/* ---- Statement document ---- */}
      {loading && (
        <div className="bg-card border border-border rounded-xl p-12 text-center">
          <Loader2 className="w-6 h-6 animate-spin mx-auto text-muted-foreground" />
          <p className="text-sm text-muted-foreground mt-3">Loading transactions…</p>
        </div>
      )}

      {!loading && hasGenerated && statementData && (
        <FinancialStatementDocument data={statementData} />
      )}

      {!loading && hasGenerated && statementData && statementData.transactions.length > 0 && (
        <div className="flex justify-center pb-4">
          <Button size="lg" onClick={handleDownloadPDF} className="gap-2">
            <Download className="w-5 h-5" />
            Download PDF
          </Button>
        </div>
      )}
    </div>
  );
}
