// ============================================
// Financial Statement PDF Generator
// Uses jsPDF + jspdf-autotable for multi-page support
// ============================================

import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import type { FinancialStatementData } from './statement';

/**
 * Format a number as currency for the PDF (plain text, not Intl).
 * Uses Indian numbering for INR, standard otherwise.
 */
function pdfCurrency(amount: number, currency: string): string {
  const symbols: Record<string, string> = { INR: '₹', USD: '$', EUR: '€', GBP: '£', JPY: '¥' };
  const sym = symbols[currency] || currency + ' ';
  const abs = Math.abs(amount);

  if (currency === 'INR') {
    const formatted = abs.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    return `${amount < 0 ? '-' : ''}${sym}${formatted}`;
  }
  const formatted = abs.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  return `${amount < 0 ? '-' : ''}${sym}${formatted}`;
}

/**
 * Format a YYYY-MM-DD date string to a readable format for PDF.
 */
function pdfDate(dateStr: string): string {
  const d = new Date(dateStr + 'T00:00:00');
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

/**
 * Generate a complete Financial Statement PDF from a FinancialStatementData object.
 * Returns a Blob.
 */
export function generateFinancialStatementPdf(data: FinancialStatementData): Blob {
  const doc = new jsPDF();
  const pageW = doc.internal.pageSize.getWidth();
  const margin = 14;
  const rightEdge = pageW - margin;
  const c = data.currency;

  // ---- HEADER ----
  doc.setFontSize(10);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(120);
  doc.text('BUDGETWISE', margin, 15);

  doc.setFontSize(22);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(30);
  const title = 'Financial Statement';
  doc.text(title, margin, 26);

  doc.setFontSize(10);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(100);
  doc.text(`${pdfDate(data.startDate)}  →  ${pdfDate(data.endDate)}`, margin, 34);
  doc.text(`Generated: ${pdfDate(data.generatedDate)}`, margin, 40);

  doc.setDrawColor(200);
  doc.setLineWidth(0.3);
  doc.line(margin, 44, rightEdge, 44);

  // ---- FINANCIAL SUMMARY ----
  doc.setFontSize(11);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(100);
  doc.text('FINANCIAL SUMMARY', margin, 52);

  autoTable(doc, {
    startY: 56,
    theme: 'plain',
    styles: { fontSize: 10, cellPadding: { top: 2, bottom: 2, left: 0, right: 0 } },
    columnStyles: {
      0: { fontStyle: 'bold', cellWidth: 50 },
      1: { halign: 'left' },
    },
    body: [
      ['Opening Balance', pdfCurrency(data.openingBalance, c)],
      ['Total Income', `+${pdfCurrency(data.stats.totalIncome, c)}`],
      ['Total Expenses', `-${pdfCurrency(data.stats.totalExpenses, c)}`],
      ['Net Change', `${data.stats.netSavings >= 0 ? '+' : ''}${pdfCurrency(data.stats.netSavings, c)}`],
      ['Closing Balance', pdfCurrency(data.closingBalance, c)],
    ],
  });

  let curY = (doc as any).lastAutoTable?.finalY || 100;

  // ---- SPENDING OVERVIEW ----
  curY += 8;
  doc.setFontSize(11);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(100);
  doc.text('SPENDING OVERVIEW', margin, curY);
  curY += 4;

  autoTable(doc, {
    startY: curY,
    theme: 'plain',
    styles: { fontSize: 10, cellPadding: { top: 2, bottom: 2, left: 0, right: 0 } },
    columnStyles: { 0: { fontStyle: 'bold', cellWidth: 55 } },
    body: [
      ['Total Transactions', String(data.stats.transactionCount)],
      ['Expense Transactions', String(data.expenseCount)],
      ['Income Transactions', String(data.incomeCount)],
      ['Avg. Daily Spending', pdfCurrency(data.averageDailySpending, c)],
      ['Largest Expense', data.largestExpense
        ? `${pdfCurrency(data.largestExpense.amount, c)} — ${data.largestExpense.merchant || data.largestExpense.description || data.largestExpense.category}`
        : '—'],
    ],
  });

  curY = (doc as any).lastAutoTable?.finalY || curY + 40;

  // ---- SPENDING BY CATEGORY ----
  if (data.categories.length > 0) {
    curY += 8;
    doc.setFontSize(11);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(100);
    doc.text('SPENDING BY CATEGORY', margin, curY);
    curY += 4;

    autoTable(doc, {
      startY: curY,
      head: [['Category', 'Count', 'Amount', '%']],
      body: data.categories.map(cat => [
        cat.category,
        String(cat.count),
        pdfCurrency(cat.amount, c),
        `${cat.percentage}%`,
      ]),
      theme: 'striped',
      headStyles: { fillColor: [55, 65, 81], fontSize: 9, fontStyle: 'bold' },
      styles: { fontSize: 9, cellPadding: 3 },
      columnStyles: {
        2: { halign: 'right' },
        3: { halign: 'right' },
      },
    });

    curY = (doc as any).lastAutoTable?.finalY || curY + 40;
  }

  // ---- TRANSACTION DETAILS ----
  curY += 8;
  doc.setFontSize(11);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(100);
  doc.text(`TRANSACTION DETAILS (${data.transactions.length})`, margin, curY);
  curY += 4;

  autoTable(doc, {
    startY: curY,
    head: [['Date', 'Description', 'Category', 'Payment', 'Amount']],
    body: data.transactions.map(t => [
      pdfDate(t.transaction_date),
      (t.merchant || t.description || t.category).substring(0, 35),
      t.category,
      t.payment_method || '—',
      `${t.type === 'income' ? '+' : '-'}${pdfCurrency(t.amount, c)}`,
    ]),
    theme: 'grid',
    headStyles: { fillColor: [55, 65, 81], fontSize: 8, fontStyle: 'bold' },
    styles: { fontSize: 8, cellPadding: 2.5 },
    columnStyles: {
      0: { cellWidth: 28 },
      4: { halign: 'right', fontStyle: 'bold' },
    },
    didParseCell(hookData) {
      if (hookData.section === 'body' && hookData.column.index === 4) {
        const t = data.transactions[hookData.row.index];
        if (t) {
          hookData.cell.styles.textColor = t.type === 'income' ? [39, 174, 96] : [192, 57, 43];
        }
      }
    },
  });

  curY = (doc as any).lastAutoTable?.finalY || curY + 40;

  // ---- INSIGHTS ----
  if (data.insights.length > 0) {
    curY += 8;
    // Check if we need a new page
    if (curY > doc.internal.pageSize.getHeight() - 50) {
      doc.addPage();
      curY = 20;
    }
    doc.setFontSize(11);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(100);
    doc.text('INSIGHTS', margin, curY);
    curY += 4;

    autoTable(doc, {
      startY: curY,
      theme: 'plain',
      styles: { fontSize: 9, cellPadding: { top: 2, bottom: 2, left: 0, right: 0 } },
      columnStyles: { 0: { fontStyle: 'bold', cellWidth: 55 } },
      body: data.insights.map(i => [i.label, `${i.value}${i.detail ? ` — ${i.detail}` : ''}`]),
    });

    curY = (doc as any).lastAutoTable?.finalY || curY + 40;
  }

  // ---- FOOTER ----
  const pageCount = doc.getNumberOfPages();
  for (let p = 1; p <= pageCount; p++) {
    doc.setPage(p);
    doc.setFontSize(7);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(150);
    const footerY = doc.internal.pageSize.getHeight() - 8;
    doc.text(`Generated by BudgetWise · All values in ${c} · For personal reference only`, margin, footerY);
    doc.text(`Page ${p} of ${pageCount}`, rightEdge, footerY, { align: 'right' });
  }

  return doc.output('blob');
}

/**
 * Build a sanitized PDF filename.
 */
export function getStatementFilename(startDate: string, endDate: string): string {
  const sanitize = (s: string) => s.replace(/[^a-zA-Z0-9-]/g, '_');
  return `BudgetWise_Statement_${sanitize(startDate)}_to_${sanitize(endDate)}.pdf`;
}
