// ============================================
// Financial Statement PDF Generator
// Uses jsPDF + jspdf-autotable for multi-page support
// ============================================

import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import type { FinancialStatementData } from './statement';
import { robotoBase64 } from './roboto';

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

  // Register Roboto font to support Unicode symbols like ₹
  doc.addFileToVFS('Roboto-Regular.ttf', robotoBase64);
  doc.addFont('Roboto-Regular.ttf', 'Roboto', 'normal');
  doc.addFont('Roboto-Regular.ttf', 'Roboto', 'bold'); // Fake bold fallback since we only embed one weight
  doc.setFont('Roboto', 'normal');

  const pageW = doc.internal.pageSize.getWidth();
  const pageH = doc.internal.pageSize.getHeight();
  const margin = 14;
  const rightEdge = pageW - margin;
  const c = data.currency;

  let curY = 15;

  // ---- HEADER ----
  doc.setFontSize(10);
  doc.setTextColor(120);
  doc.text('BUDGETWISE', margin, curY);

  curY += 10;
  doc.setFontSize(22);
  doc.setFont('Roboto', 'bold');
  doc.setTextColor(30);
  doc.text('Financial Statement', margin, curY);

  curY += 8;
  doc.setFontSize(10);
  doc.setFont('Roboto', 'normal');
  doc.setTextColor(100);
  doc.text(`${pdfDate(data.startDate)}  →  ${pdfDate(data.endDate)}`, margin, curY);
  doc.text(`Generated: ${pdfDate(data.generatedDate)}`, rightEdge, curY, { align: 'right' });

  curY += 6;
  doc.setDrawColor(200);
  doc.setLineWidth(0.3);
  doc.line(margin, curY, rightEdge, curY);
  curY += 8;

  const defaultStyles = { font: 'Roboto', fontSize: 10, textColor: 40 };

  // ---- FINANCIAL SUMMARY ----
  doc.setFontSize(11);
  doc.setFont('Roboto', 'bold');
  doc.setTextColor(100);
  doc.text('FINANCIAL SUMMARY', margin, curY);
  curY += 4;

  autoTable(doc, {
    startY: curY,
    theme: 'plain',
    styles: { ...defaultStyles, cellPadding: { top: 2, bottom: 2, left: 0, right: 0 } },
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

  curY = (doc as any).lastAutoTable?.finalY + 8 || curY + 40;

  // ---- SPENDING OVERVIEW ----
  doc.setFontSize(11);
  doc.setFont('Roboto', 'bold');
  doc.setTextColor(100);
  doc.text('SPENDING OVERVIEW', margin, curY);
  curY += 4;

  autoTable(doc, {
    startY: curY,
    theme: 'plain',
    styles: { ...defaultStyles, cellPadding: { top: 2, bottom: 2, left: 0, right: 0 } },
    columnStyles: { 0: { fontStyle: 'bold', cellWidth: 55 } },
    body: [
      ['Transactions', String(data.stats.transactionCount)],
      ['Income transactions', String(data.incomeCount)],
      ['Expense transactions', String(data.expenseCount)],
      ['Average daily spending', pdfCurrency(data.averageDailySpending, c)],
      ['Largest expense', data.largestExpense
        ? `${pdfCurrency(data.largestExpense.amount, c)} — ${data.largestExpense.merchant || data.largestExpense.description || data.largestExpense.category}`
        : '—'],
    ],
  });

  curY = (doc as any).lastAutoTable?.finalY + 8 || curY + 40;

  // ---- SPENDING BY CATEGORY ----
  if (data.categories.length > 0) {
    if (curY > pageH - 50) { doc.addPage(); curY = 20; }
    
    doc.setFontSize(11);
    doc.setFont('Roboto', 'bold');
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
      theme: 'grid',
      headStyles: { font: 'Roboto', fillColor: [240, 240, 240], textColor: [80, 80, 80], fontSize: 9, fontStyle: 'bold', lineWidth: 0.1, lineColor: [200, 200, 200] },
      styles: { ...defaultStyles, fontSize: 9, cellPadding: 3, lineWidth: 0.1, lineColor: [200, 200, 200] },
      columnStyles: {
        1: { halign: 'center' },
        2: { halign: 'right' },
        3: { halign: 'right' },
      },
    });

    curY = (doc as any).lastAutoTable?.finalY + 8 || curY + 40;
  }

  // ---- TRANSACTION DETAILS ----
  if (data.transactions.length > 0) {
    // Start transaction details on a new page if the current page is getting full
    if (curY > pageH - 80) { doc.addPage(); curY = 20; }
    
    doc.setFontSize(11);
    doc.setFont('Roboto', 'bold');
    doc.setTextColor(100);
    doc.text(`TRANSACTION DETAILS (${data.transactions.length})`, margin, curY);
    curY += 4;

    autoTable(doc, {
      startY: curY,
      head: [['Date', 'Description', 'Category', 'Payment', 'Amount']],
      body: data.transactions.map(t => [
        pdfDate(t.transaction_date),
        (t.merchant || t.description || t.category).substring(0, 40),
        t.category,
        t.payment_method || '—',
        `${t.type === 'income' ? '+' : '-'}${pdfCurrency(t.amount, c)}`,
      ]),
      theme: 'grid',
      headStyles: { font: 'Roboto', fillColor: [240, 240, 240], textColor: [80, 80, 80], fontSize: 8, fontStyle: 'bold', lineWidth: 0.1, lineColor: [200, 200, 200] },
      styles: { ...defaultStyles, fontSize: 8, cellPadding: 2.5, lineWidth: 0.1, lineColor: [200, 200, 200] },
      columnStyles: {
        0: { cellWidth: 26 },
        2: { cellWidth: 26 },
        3: { cellWidth: 20 },
        4: { halign: 'right', fontStyle: 'bold', cellWidth: 26 },
      },
      didParseCell(hookData) {
        if (hookData.section === 'body' && hookData.column.index === 4) {
          const t = data.transactions[hookData.row.index];
          if (t) {
            // Use softer colors for printability, not dark-mode UI colors
            hookData.cell.styles.textColor = t.type === 'income' ? [30, 110, 60] : [180, 40, 40];
          }
        }
      },
    });
    
    curY = (doc as any).lastAutoTable?.finalY + 8 || curY + 40;
  }

  // ---- WHAT STOOD OUT (INSIGHTS) ----
  if (data.insights.length > 0) {
    // Only page break if not enough space
    if (curY > pageH - 40) { doc.addPage(); curY = 20; }
    
    doc.setFontSize(11);
    doc.setFont('Roboto', 'bold');
    doc.setTextColor(100);
    doc.text('WHAT STOOD OUT', margin, curY);
    curY += 4;

    autoTable(doc, {
      startY: curY,
      theme: 'plain',
      styles: { ...defaultStyles, fontSize: 9, cellPadding: { top: 2, bottom: 2, left: 0, right: 0 } },
      columnStyles: { 0: { fontStyle: 'bold', cellWidth: 55 } },
      body: data.insights.map(i => [i.label, `${i.value}${i.detail ? ` — ${i.detail}` : ''}`]),
    });

    curY = (doc as any).lastAutoTable?.finalY + 8 || curY + 40;
  }

  // ---- FOOTER ----
  const pageCount = doc.getNumberOfPages();
  for (let p = 1; p <= pageCount; p++) {
    doc.setPage(p);
    doc.setFontSize(8);
    doc.setFont('Roboto', 'normal');
    doc.setTextColor(150);
    const footerY = pageH - 10;
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
