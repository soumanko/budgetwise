package com.soumanko.budgetwise.domain.finance

object Categories {
    val EXPENSE_CATEGORIES = listOf(
        "Food", "Travel", "Shopping", "Entertainment", "Education", 
        "Bills", "Health", "Groceries", "Rent", "Subscriptions", 
        "Personal", "Other"
    )

    val INCOME_CATEGORIES = listOf(
        "Money from Home", "Salary", "Freelancing", "Scholarship", 
        "Refund", "Gift", "Other"
    )

    val PAYMENT_METHODS = listOf(
        "UPI", "Cash", "Debit Card", "Credit Card", "Bank Transfer", "Other"
    )
    
    val ALL_CATEGORIES = INCOME_CATEGORIES + EXPENSE_CATEGORIES
}
