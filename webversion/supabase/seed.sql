-- ============================================
-- Sample Seed Data for BudgetWise
-- Run AFTER signing up to populate your account
-- Replace USER_ID_HERE with your actual user ID
-- Find it in Supabase: Authentication > Users
-- ============================================

-- INSTRUCTIONS:
-- 1. Sign up in the app
-- 2. Go to Supabase > Authentication > Users
-- 3. Copy your user's UUID
-- 4. Replace all instances of USER_ID_HERE below
-- 5. Run this SQL in Supabase SQL Editor

-- Get the account ID for the user
-- (The trigger auto-creates a 'Primary Account' on signup)

DO $$
DECLARE
  v_user_id UUID := 'USER_ID_HERE'; -- REPLACE THIS
  v_account_id UUID;
BEGIN

  -- Get the auto-created account
  SELECT id INTO v_account_id FROM public.accounts WHERE user_id = v_user_id LIMIT 1;
  
  IF v_account_id IS NULL THEN
    RAISE EXCEPTION 'No Primary Account found for user %. Please ensure the handle_new_user trigger worked or create an account first.', v_user_id;
  END IF;

  -- ============================================
  -- PREVENT DUPLICATES (Clear existing data for this user)
  -- ============================================
  DELETE FROM public.transactions WHERE user_id = v_user_id;
  DELETE FROM public.budgets WHERE user_id = v_user_id;
  DELETE FROM public.recurring_expenses WHERE user_id = v_user_id;
  DELETE FROM public.savings_goals WHERE user_id = v_user_id;

  -- ============================================
  -- INCOME TRANSACTIONS
  -- ============================================
  INSERT INTO public.transactions (user_id, account_id, type, amount, category, description, transaction_date) VALUES
  (v_user_id, v_account_id, 'income', 15000, 'Money from Home', 'August allowance', '2026-08-01'),
  (v_user_id, v_account_id, 'income', 5000, 'Freelancing', 'Logo design project', '2026-08-05'),
  (v_user_id, v_account_id, 'income', 2000, 'Scholarship', 'Monthly scholarship', '2026-08-03');

  -- ============================================
  -- EXPENSE TRANSACTIONS
  -- ============================================
  INSERT INTO public.transactions (user_id, account_id, type, amount, category, description, merchant, payment_method, transaction_date) VALUES
  (v_user_id, v_account_id, 'expense', 180, 'Food', 'Lunch', 'Zomato', 'UPI', '2026-08-01'),
  (v_user_id, v_account_id, 'expense', 120, 'Food', 'Evening snacks', 'Swiggy', 'UPI', '2026-08-01'),
  (v_user_id, v_account_id, 'expense', 80, 'Travel', 'Auto to college', NULL, 'Cash', '2026-08-02'),
  (v_user_id, v_account_id, 'expense', 450, 'Shopping', 'New earphones', 'Amazon', 'Debit Card', '2026-08-02'),
  (v_user_id, v_account_id, 'expense', 200, 'Food', 'Dinner with friends', 'Dominos', 'UPI', '2026-08-02'),
  (v_user_id, v_account_id, 'expense', 60, 'Food', 'Coffee', 'Starbucks', 'UPI', '2026-08-03'),
  (v_user_id, v_account_id, 'expense', 300, 'Entertainment', 'Movie tickets', 'BookMyShow', 'UPI', '2026-08-03'),
  (v_user_id, v_account_id, 'expense', 150, 'Food', 'Lunch', NULL, 'Cash', '2026-08-03'),
  (v_user_id, v_account_id, 'expense', 500, 'Education', 'Online course', 'Udemy', 'Debit Card', '2026-08-04'),
  (v_user_id, v_account_id, 'expense', 100, 'Travel', 'Bus fare', NULL, 'Cash', '2026-08-04'),
  (v_user_id, v_account_id, 'expense', 250, 'Food', 'Groceries', 'BigBasket', 'UPI', '2026-08-04'),
  (v_user_id, v_account_id, 'expense', 1200, 'Shopping', 'T-shirt and jeans', 'Myntra', 'Debit Card', '2026-08-05'),
  (v_user_id, v_account_id, 'expense', 180, 'Food', 'Lunch', NULL, 'UPI', '2026-08-05'),
  (v_user_id, v_account_id, 'expense', 399, 'Bills', 'Phone recharge', 'Jio', 'UPI', '2026-08-05'),
  (v_user_id, v_account_id, 'expense', 80, 'Travel', 'Metro', NULL, 'Cash', '2026-08-06'),
  (v_user_id, v_account_id, 'expense', 350, 'Food', 'Weekend brunch', 'Cafe Coffee Day', 'UPI', '2026-08-06'),
  (v_user_id, v_account_id, 'expense', 649, 'Subscriptions', 'Netflix', 'Netflix', 'Debit Card', '2026-08-06'),
  (v_user_id, v_account_id, 'expense', 119, 'Subscriptions', 'Spotify', 'Spotify', 'Debit Card', '2026-08-06'),
  (v_user_id, v_account_id, 'expense', 200, 'Personal', 'Haircut', NULL, 'Cash', '2026-08-07'),
  (v_user_id, v_account_id, 'expense', 160, 'Food', 'Dinner', 'Swiggy', 'UPI', '2026-08-07'),
  (v_user_id, v_account_id, 'expense', 1500, 'Health', 'Doctor visit', NULL, 'UPI', '2026-08-07');

  -- ============================================
  -- BUDGETS
  -- ============================================
  INSERT INTO public.budgets (user_id, category, amount, month) VALUES
  (v_user_id, 'Food', 4000, '2026-08-01'),
  (v_user_id, 'Travel', 2000, '2026-08-01'),
  (v_user_id, 'Shopping', 2000, '2026-08-01'),
  (v_user_id, 'Entertainment', 1500, '2026-08-01'),
  (v_user_id, 'Education', 1000, '2026-08-01'),
  (v_user_id, 'Subscriptions', 1000, '2026-08-01');

  -- ============================================
  -- RECURRING EXPENSES
  -- ============================================
  INSERT INTO public.recurring_expenses (user_id, name, amount, category, frequency, next_due_date) VALUES
  (v_user_id, 'Netflix', 649, 'Subscriptions', 'monthly', '2026-09-06'),
  (v_user_id, 'Spotify', 119, 'Subscriptions', 'monthly', '2026-09-06'),
  (v_user_id, 'Phone Recharge', 399, 'Bills', 'monthly', '2026-09-05'),
  (v_user_id, 'Gym Membership', 800, 'Health', 'monthly', '2026-08-15');

  -- ============================================
  -- SAVINGS GOALS
  -- ============================================
  INSERT INTO public.savings_goals (user_id, name, target_amount, current_amount, deadline, description) VALUES
  (v_user_id, 'New Laptop', 80000, 35000, '2027-03-01', 'MacBook Air for development'),
  (v_user_id, 'Emergency Fund', 50000, 12000, NULL, 'Three months of expenses'),
  (v_user_id, 'Weekend Trip', 15000, 5000, '2026-10-15', 'Goa trip with friends');

  -- ============================================
  -- UPDATE PROFILE
  -- ============================================
  UPDATE public.profiles SET
    full_name = 'Manko',
    monthly_budget = 15000,
    low_balance_threshold = 1000
  WHERE user_id = v_user_id;

END $$;
