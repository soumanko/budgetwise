import { NextResponse } from 'next/server';
import { fetchGroqCompletion } from '@/lib/ai/groq';

export async function GET() {
  try {
    console.log('[DEBUG AI HEALTH] Checking environment...');
    const hasApiKey = !!process.env.GROQ_API_KEY;
    console.log('[DEBUG AI HEALTH] GROQ_API_KEY exists:', hasApiKey);

    if (!hasApiKey) {
      return NextResponse.json({ status: 'error', message: 'GROQ_API_KEY is missing' }, { status: 500 });
    }

    const modelName = process.env.GROQ_MODEL || 'openai/gpt-oss-20b';
    console.log('[DEBUG AI HEALTH] Testing model:', modelName);
    
    console.log('[DEBUG AI HEALTH] Sending ping request to Groq...');
    const responseText = await fetchGroqCompletion('You are a helpful assistant.', 'Ping. Reply with exactly "Pong".');
    console.log('[DEBUG AI HEALTH] Received response:', responseText);

    return NextResponse.json({ 
      status: 'ok', 
      provider: 'Groq',
      model: modelName,
      message: 'Groq API is reachable',
      test_response: responseText
    });
  } catch (error: any) {
    console.error('[DEBUG AI HEALTH] Error:', {
      name: error?.name,
      message: error?.message,
      status: error?.status,
    });
    return NextResponse.json({ 
      status: 'error', 
      message: error?.message || 'Unknown error',
      details: { name: error?.name, status: error?.status }
    }, { status: 500 });
  }
}
