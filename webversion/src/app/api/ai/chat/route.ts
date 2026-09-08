import { NextRequest, NextResponse } from 'next/server';
import { createServerSupabaseClient } from '@/lib/supabase/server';
import { fetchGroqCompletion, FINANCIAL_ASSISTANT_SYSTEM_PROMPT } from '@/lib/ai/groq';
import { calculateMonthlyStats, calculateCategorySpending, calculateBalance } from '@/lib/finance/calculations';
import { getMonthStart, getMonthEnd, formatCurrency } from '@/lib/utils';

export async function POST(request: NextRequest) {
  try {
    const supabase = await createServerSupabaseClient();
    const { data: { user } } = await supabase.auth.getUser();

    if (!user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const { message } = await request.json();
    if (!message || typeof message !== 'string') {
      return NextResponse.json({ error: 'Message is required' }, { status: 400 });
    }

    console.log('[DEBUG AI CHAT] Boolean(process.env.GROQ_API_KEY):', Boolean(process.env.GROQ_API_KEY));
    console.log('[DEBUG AI CHAT] process.env.GROQ_MODEL:', process.env.GROQ_MODEL);

    const apiKey = process.env.GROQ_API_KEY;
    if (!apiKey) {
      return NextResponse.json({
        response: 'Groq AI is not configured.',
      });
    }

    // Fetch user's financial data (aggregated, not raw personal data)
    const monthStart = getMonthStart();
    const monthEnd = getMonthEnd();

    const [allTxRes, monthTxRes, profileRes, budgetRes, recurringRes, goalsRes] = await Promise.all([
      supabase.from('transactions').select('type, amount, category, transaction_date').eq('user_id', user.id).order('transaction_date', { ascending: false }).limit(500),
      supabase.from('transactions').select('type, amount, category, transaction_date').eq('user_id', user.id).gte('transaction_date', monthStart).lte('transaction_date', monthEnd),
      supabase.from('profiles').select('full_name, currency, monthly_budget').eq('user_id', user.id).single(),
      supabase.from('budgets').select('category, amount').eq('user_id', user.id).eq('month', monthStart),
      supabase.from('recurring_expenses').select('name, amount, frequency, next_due_date').eq('user_id', user.id).eq('active', true),
      supabase.from('savings_goals').select('name, target_amount, current_amount, deadline').eq('user_id', user.id),
    ]);

    const allTransactions = allTxRes.data || [];
    const monthTransactions = monthTxRes.data || [];
    const profile = profileRes.data;
    const budgets = budgetRes.data || [];
    const recurring = recurringRes.data || [];
    const goals = goalsRes.data || [];

    // Build aggregated context
    const balance = calculateBalance(allTransactions as never[]);
    const monthStats = calculateMonthlyStats(monthTransactions as never[]);
    const categorySpending = calculateCategorySpending(monthTransactions as never[]);

    const currency = profile?.currency || 'INR';
    const budgetInfo = budgets.map(b => `${b.category}: ${formatCurrency(b.amount, currency)} budget`).join(', ');
    const categoryInfo = categorySpending.map(c => `${c.category}: ${formatCurrency(c.amount, currency)} (${c.percentage}%)`).join(', ');
    const recurringInfo = recurring.map(r => `${r.name}: ${formatCurrency(r.amount, currency)}/${r.frequency}`).join(', ');
    const goalsInfo = goals.map(g => `${g.name}: ${formatCurrency(g.current_amount, currency)}/${formatCurrency(g.target_amount, currency)}`).join(', ');

    const context = `
USER FINANCIAL CONTEXT (as of today):
- Name: ${profile?.full_name || 'User'}
- Current Balance: ${formatCurrency(balance, currency)}
- This Month's Income: ${formatCurrency(monthStats.totalIncome, currency)}
- This Month's Expenses: ${formatCurrency(monthStats.totalExpenses, currency)}
- This Month's Savings: ${formatCurrency(monthStats.netSavings, currency)}
- Savings Rate: ${monthStats.savingsRate}%
- Total Transactions This Month: ${monthStats.transactionCount}
- Monthly Budget: ${profile?.monthly_budget ? formatCurrency(profile.monthly_budget, currency) : 'Not set'}
- Category Spending This Month: ${categoryInfo || 'No expenses yet'}
- Category Budgets: ${budgetInfo || 'No budgets set'}
- Active Recurring Expenses: ${recurringInfo || 'None'}
- Savings Goals: ${goalsInfo || 'None'}
`;

    console.log('[DEBUG AI CHAT] Sending request to Groq...');
    const userPrompt = `${context}\n\nUser question: ${message}`;
    const responseText = await fetchGroqCompletion(FINANCIAL_ASSISTANT_SYSTEM_PROMPT, userPrompt);
    console.log('[DEBUG AI CHAT] Received response from Groq.');
    
    return NextResponse.json({ response: responseText });
  } catch (error: any) {
    console.error('[DEBUG AI CHAT] Error caught in try-catch:');
    console.error({
      name: error?.name,
      message: error?.message,
      status: error?.status,
      code: error?.code,
    });
    return NextResponse.json({
      response: error?.message || 'The AI request timed out. Please try again.',
      error: error?.message,
    }, { status: 500 });
  }
}
