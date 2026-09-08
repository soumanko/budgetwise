import { NextRequest, NextResponse } from 'next/server';
import { createServerSupabaseClient } from '@/lib/supabase/server';
import { fetchGroqCompletion } from '@/lib/ai/groq';
import { calculateMonthlyStats, calculateCategorySpending, calculateBalance } from '@/lib/finance/calculations';
import { getMonthStart, getMonthEnd, formatCurrency, getPreviousMonthDateStr } from '@/lib/utils';

export async function POST(request: NextRequest) {
  try {
    const supabase = await createServerSupabaseClient();
    const { data: { user } } = await supabase.auth.getUser();

    if (!user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const apiKey = process.env.GROQ_API_KEY;
    if (!apiKey) {
      return NextResponse.json({
        insights: [],
        message: 'AI insights require GROQ_API_KEY configuration',
      });
    }

    const monthStart = getMonthStart();
    const monthEnd = getMonthEnd();

    const [monthTxRes, profileRes] = await Promise.all([
      supabase.from('transactions').select('type, amount, category').eq('user_id', user.id).gte('transaction_date', monthStart).lte('transaction_date', monthEnd),
      supabase.from('profiles').select('currency').eq('user_id', user.id).single(),
    ]);

    const monthStats = calculateMonthlyStats((monthTxRes.data || []) as never[]);
    const categorySpending = calculateCategorySpending((monthTxRes.data || []) as never[]);
    const currency = profileRes.data?.currency || 'INR';

    const categoryInfo = categorySpending.map(c => `${c.category}: ${formatCurrency(c.amount, currency)} (${c.percentage}%)`).join(', ');

    const systemPrompt = `Based on this financial data, generate 3 brief, actionable financial tips. Each tip should be 1-2 sentences max. Format as a JSON array of objects with "title" and "description" fields. Respond ONLY with the JSON array, no other text.`;
    const userPrompt = `Monthly Income: ${formatCurrency(monthStats.totalIncome, currency)}
Monthly Expenses: ${formatCurrency(monthStats.totalExpenses, currency)}
Savings Rate: ${monthStats.savingsRate}%
Spending by Category: ${categoryInfo || 'No data'}`;

    const responseText = await fetchGroqCompletion(systemPrompt, userPrompt);

    let insights = [];
    try {
      const text = responseText.replace(/```json\n?/g, '').replace(/```\n?/g, '').trim();
      insights = JSON.parse(text);
    } catch {
      insights = [];
    }

    // Store insights
    if (insights.length > 0) {
      // Clear old insights for this user
      await supabase.from('financial_insights').delete().eq('user_id', user.id);

      const insightRows = insights.map((i: { title: string; description: string }) => ({
        user_id: user.id,
        insight_type: 'ai_recommendation',
        title: i.title,
        description: i.description,
        severity: 'info' as const,
        data: null,
      }));

      await supabase.from('financial_insights').insert(insightRows);
    }

    return NextResponse.json({ insights });
  } catch (error) {
    console.error('AI insights error:', error);
    return NextResponse.json({ insights: [], error: 'Failed to generate insights' }, { status: 500 });
  }
}
