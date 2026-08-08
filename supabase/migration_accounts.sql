-- ============================================
-- Migration: Add Accounts Table and Fix Schema
-- Run this in Supabase SQL Editor
-- ============================================

-- 1. Create the accounts table if it doesn't exist
CREATE TABLE IF NOT EXISTS public.accounts (
  id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  name TEXT NOT NULL DEFAULT 'Primary Account',
  account_type TEXT NOT NULL DEFAULT 'Cash' CHECK (account_type IN ('Bank', 'Cash', 'Savings', 'Wallet', 'Other')),
  opening_balance NUMERIC(12,2) NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Ensure index exists
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON public.accounts(user_id);

-- 2. Secure the table with RLS
ALTER TABLE public.accounts ENABLE ROW LEVEL SECURITY;

-- Safely drop existing policies if any to prevent duplicate policy errors
DROP POLICY IF EXISTS "Users can view own accounts" ON public.accounts;
DROP POLICY IF EXISTS "Users can insert own accounts" ON public.accounts;
DROP POLICY IF EXISTS "Users can update own accounts" ON public.accounts;
DROP POLICY IF EXISTS "Users can delete own accounts" ON public.accounts;

-- Recreate policies strictly bound to auth.uid()
CREATE POLICY "Users can view own accounts"
  ON public.accounts FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own accounts"
  ON public.accounts FOR INSERT
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own accounts"
  ON public.accounts FOR UPDATE
  USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own accounts"
  ON public.accounts FOR DELETE
  USING (auth.uid() = user_id);

-- 3. Ensure transactions table has the account_id column and foreign key
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='transactions' AND column_name='account_id') THEN
    ALTER TABLE public.transactions ADD COLUMN account_id UUID REFERENCES public.accounts(id) ON DELETE SET NULL;
  END IF;
END $$;

-- 4. Recreate the trigger for new user signups to ensure they get a Profile AND an Account
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  -- Create profile
  INSERT INTO public.profiles (user_id, full_name)
  VALUES (NEW.id, COALESCE(NEW.raw_user_meta_data->>'full_name', ''));

  -- Create default primary account
  INSERT INTO public.accounts (user_id, name, account_type, opening_balance)
  VALUES (NEW.id, 'Primary Account', 'Cash', 0);

  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5. Backfill script for existing users who might have missed the trigger
DO $$
DECLARE
  u RECORD;
  new_account_id UUID;
BEGIN
  FOR u IN SELECT id FROM auth.users LOOP
    -- If user does not have ANY account, create one
    IF NOT EXISTS (SELECT 1 FROM public.accounts WHERE user_id = u.id) THEN
      INSERT INTO public.accounts (user_id, name, account_type, opening_balance)
      VALUES (u.id, 'Primary Account', 'Cash', 0)
      RETURNING id INTO new_account_id;

      -- Assign any orphaned transactions to this newly created account
      UPDATE public.transactions SET account_id = new_account_id WHERE user_id = u.id AND account_id IS NULL;
    ELSE
      -- Even if they have an account, make sure all their transactions point to their first account if null
      SELECT id INTO new_account_id FROM public.accounts WHERE user_id = u.id LIMIT 1;
      UPDATE public.transactions SET account_id = new_account_id WHERE user_id = u.id AND account_id IS NULL;
    END IF;
  END LOOP;
END $$;
