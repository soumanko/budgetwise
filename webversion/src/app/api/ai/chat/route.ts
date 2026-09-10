import { NextRequest, NextResponse } from 'next/server';
import { createServerSupabaseClient } from '@/lib/supabase/server';
import { fetchGroqCompletion, FINANCIAL_ASSISTANT_SYSTEM_PROMPT } from '@/lib/ai/groq';
import { calculateMonthlyStats, calculateCategorySpending, calculateBalance } from '@/lib/finance/calculations';
import { getMonthStart, getMonthEnd, formatCurrency } from '@/lib/utils';

import { createClient } from '@supabase/supabase-js';

export async function POST(request: NextRequest) {
  try {
    let supabase = await createServerSupabaseClient();
    let user;

    const authHeader = request.headers.get('Authorization');
    console.log('[DEBUG AI CHAT] Authorization header exists:', !!authHeader);

    if (authHeader && authHeader.startsWith('Bearer ')) {
      const token = authHeader.substring(7);
      
      // Re-create the supabase client with the explicit Authorization header for PostgREST
      supabase = createClient(
        process.env.NEXT_PUBLIC_SUPABASE_URL || '',
        process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || '',
        {
          global: {
            headers: {
              Authorization: authHeader,
            },
          },
        }
      ) as any;

      const { data, error } = await supabase.auth.getUser();
      if (error) {
         console.log('[DEBUG AI CHAT] getUser error:', error.message);
      }
      user = data?.user;
    } else {
      const { data } = await supabase.auth.getUser();
      user = data?.user;
    }

    console.log('[DEBUG AI CHAT] Authentication succeeded:', !!user);
    if (user) {
      console.log('[DEBUG AI CHAT] Authenticated user ID:', user.id);
    }

    if (!user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const { message } = await request.json();
    if (!message || typeof message !== 'string') {
      return NextResponse.json({ error: 'Message is required' }, { status: 400 });
    }

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

    if (allTxRes.error) console.error('[DEBUG AI CHAT] allTxRes error:', allTxRes.error);
    if (profileRes.error) console.error('[DEBUG AI CHAT] profileRes error:', profileRes.error);

    const allTransactions = allTxRes.data || [];
    const monthTransactions = monthTxRes.data || [];
    const profile = profileRes.data;
    const budgets = budgetRes.data || [];
    const recurring = recurringRes.data || [];
    const goals = goalsRes.data || [];

    console.log('[DEBUG AI CHAT] Transaction query result count:', allTransactions.length);
    console.log('[DEBUG AI CHAT] Profile query success:', !!profile);
    
    // Build aggregated context
    const balance = calculateBalance(allTransactions as never[]);
    const monthStats = calculateMonthlyStats(monthTransactions as never[]);
    const categorySpending = calculateCategorySpending(monthTransactions as never[]);

    const currency = profile?.currency || 'INR';
    const budgetInfo = budgets.map(b => `${b.category}: ${formatCurrency(b.amount, currency)} budget`).join(', ');
    const categoryInfo = categorySpending.map(c => `${c.category}: ${formatCurrency(c.amount, currency)} (${c.percentage}%)`).join(', ');
    const recurringInfo = recurring.map(r => `${r.name}: ${formatCurrency(r.amount, currency)}/${r.frequency}`).join(', ');
    const goalsInfo = goals.map(g => `${g.name}: ${formatCurrency(g.current_amount, currency)}/${formatCurrency(g.target_amount, currency)}`).join(', ');

    console.log('[DEBUG AI CHAT] Final Context Stats -> Total Tx:', allTransactions.length, 'Month Tx:', monthTransactions.length, 'Budgets:', budgets.length, 'Recurring:', recurring.length, 'Goals:', goals.length);

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
