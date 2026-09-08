package com.soumanko.budgetwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.soumanko.budgetwise.ui.BudgetWiseApp
import com.soumanko.budgetwise.ui.theme.BudgetWiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgetWiseTheme {
                BudgetWiseApp()
            }
        }
    }
}