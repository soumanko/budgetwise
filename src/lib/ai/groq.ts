export const FINANCIAL_ASSISTANT_SYSTEM_PROMPT = `You are BudgetWise AI, a helpful and friendly personal financial assistant.

IMPORTANT RULES:
1. You are NOT a licensed financial advisor. Never claim to be one.
2. Never guarantee investment returns or give specific investment instructions.
3. Base your answers ONLY on the user's financial data provided in the context.
4. If data is missing or insufficient, say so clearly.
5. Always distinguish between "recorded data" and "estimated/predicted data."
6. Never invent or fabricate transactions, balances, or financial information.
7. Use the Indian Rupee (₹) symbol for amounts unless the user uses a different currency.
8. Be concise and helpful. Use bullet points where appropriate.
9. If asked about something outside your scope (investments, stocks, crypto), politely redirect to the user's spending data.

FORMAT:
- Use clear, simple language
- Format currency amounts properly
- Use bullet points for lists
- Keep responses concise but informative`;

export async function fetchGroqCompletion(systemPrompt: string, userPrompt: string) {
  const apiKey = process.env.GROQ_API_KEY;
  if (!apiKey) {
    throw new Error('Groq AI is not configured.');
  }

  const model = process.env.GROQ_MODEL || 'openai/gpt-oss-20b';

  try {
    const response = await fetch('https://api.groq.com/openai/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${apiKey}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model: model,
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: userPrompt }
        ]
      })
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error('[DEBUG AI API] Groq HTTP Error:', response.status, errorText, 'Model:', model);
      
      if (response.status === 401 || response.status === 403) {
        throw new Error('Groq authentication failed. Please check the API key.');
      }
      if (response.status === 404) {
        throw new Error('The configured Groq model is currently unavailable.');
      }
      if (response.status === 429) {
        throw new Error('AI usage limit reached. Please try again later.');
      }
      if (response.status >= 500) {
        throw new Error('The AI service is temporarily unavailable.');
      }
      throw new Error(`Groq error: ${response.status}`);
    }

    const data = await response.json();
    if (!data.choices || data.choices.length === 0) {
      console.error('[DEBUG AI API] Groq Response missing choices:', data);
      throw new Error('The AI service is temporarily unavailable.');
    }

    return data.choices[0].message.content;
  } catch (err: any) {
    console.error('[DEBUG AI API] Fetch/Network Error:', err.message);
    if (err.message.includes('Groq AI is not configured.') || 
        err.message.includes('Groq authentication failed') ||
        err.message.includes('usage limit reached') ||
        err.message.includes('model is currently unavailable') ||
        err.message.includes('AI service is temporarily unavailable')) {
      throw err;
    }
    throw new Error('The AI request timed out. Please try again.');
  }
}
