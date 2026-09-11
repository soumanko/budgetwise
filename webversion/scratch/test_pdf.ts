import { generateFinancialStatementPdf } from '../src/lib/finance/pdf';
import { buildFinancialStatement } from '../src/lib/finance/statement';

const dummyData = buildFinancialStatement(
  [], // transactions
  1000, // openingBalance
  '2026-09-01',
  '2026-09-11',
  'INR'
);

try {
  console.log('Generating PDF...');
  const blob = generateFinancialStatementPdf(dummyData);
  console.log('PDF generated successfully. Blob size:', blob.size);
} catch (e) {
  console.error('PDF generation failed:', e);
}
