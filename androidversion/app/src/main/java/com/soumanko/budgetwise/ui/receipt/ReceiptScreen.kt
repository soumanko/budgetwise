package com.soumanko.budgetwise.ui.receipt

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soumanko.budgetwise.domain.finance.DateUtils
import com.soumanko.budgetwise.domain.finance.toINR
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    viewModel: ReceiptViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Receipt") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (uiState is ReceiptUiState.Success) {
                val state = uiState as ReceiptUiState.Success
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareText = generateShareText(state)
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = uiState) {
                is ReceiptUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ReceiptUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadData() }) {
                            Text("Retry")
                        }
                    }
                }
                is ReceiptUiState.Success -> {
                    ReceiptPaper(state)
                }
            }
        }
    }
}

@Composable
fun ReceiptPaper(state: ReceiptUiState.Success) {
    val paperColor = Color(0xFFF9F9F9)
    val inkColor = Color(0xFF1E1E1E)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(top = 16.dp, bottom = 32.dp)
            .drawBehind {
                val toothSize = 8.dp.toPx()
                val path = Path()
                
                // Top torn edge
                var currentX = 0f
                path.moveTo(0f, toothSize)
                while (currentX < size.width) {
                    currentX += toothSize
                    path.lineTo(currentX - (toothSize / 2), 0f)
                    path.lineTo(currentX, toothSize)
                }
                
                // Right edge
                path.lineTo(size.width, size.height - toothSize)
                
                // Bottom torn edge
                while (currentX > 0f) {
                    currentX -= toothSize
                    path.lineTo(currentX + (toothSize / 2), size.height)
                    path.lineTo(currentX, size.height - toothSize)
                }
                
                // Left edge
                path.lineTo(0f, toothSize)
                path.close()
                
                drawPath(
                    path = path,
                    color = paperColor
                )
            }
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "BUDGETWISE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = inkColor,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Spend Smarter. Live Better.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = inkColor
                )
                Spacer(modifier = Modifier.height(16.dp))
                DashedDivider(color = inkColor)
                Spacer(modifier = Modifier.height(16.dp))
                
                ReceiptInfoRow("DATE:", DateUtils.getTodayDateStr(), inkColor)
                val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                ReceiptInfoRow("DAY:", dayFormat.format(Date()), inkColor)
                
                Spacer(modifier = Modifier.height(16.dp))
                DashedDivider(color = inkColor)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DESCRIPTION", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = inkColor, fontWeight = FontWeight.Bold)
                    Text("AMOUNT", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = inkColor, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(state.transactions) { tx ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = (tx.merchant ?: tx.category).take(20),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = inkColor,
                        maxLines = 1
                    )
                    val isIncome = tx.type == "income"
                    Text(
                        text = "${if (isIncome) "+" else "-"}${tx.amount.toINR()}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = inkColor
                    )
                }
            }
            
            if (state.transactions.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No transactions today.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = inkColor,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                DashedDivider(color = inkColor)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TOTAL SPENT",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = inkColor
                    )
                    Text(
                        text = state.totalExpenses.toINR(),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = inkColor
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                DashedDivider(color = inkColor)
                Spacer(modifier = Modifier.height(16.dp))
                
                ReceiptInfoRow("TRANSACTIONS", state.transactions.size.toString(), inkColor)
                ReceiptInfoRow("PAYMENT METHODS", state.paymentMethodCount.toString(), inkColor)
                ReceiptInfoRow("TOP CATEGORY", state.topCategory ?: "-", inkColor)
                
                Spacer(modifier = Modifier.height(16.dp))
                DashedDivider(color = inkColor)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Small steps add up\nto big goals.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = inkColor,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                BarcodeVisual(color = inkColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "BUDGETWISE\n${DateUtils.getTodayDateStr()}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = inkColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ReceiptInfoRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = color)
        Text(text = value, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = color)
    }
}

@Composable
fun DashedDivider(color: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }
}

@Composable
fun BarcodeVisual(color: Color) {
    Canvas(modifier = Modifier.fillMaxWidth(0.8f).height(40.dp)) {
        val barWidths = listOf(2f, 4f, 1f, 3f, 2f, 5f, 1f, 2f, 4f, 1f, 3f, 2f)
        var currentX = 0f
        val spacing = 6f
        
        while (currentX < size.width) {
            val width = barWidths[(currentX / (spacing + 2f)).toInt() % barWidths.size]
            drawRect(
                color = color,
                topLeft = Offset(currentX, 0f),
                size = Size(width * 2f, size.height)
            )
            currentX += width * 2f + spacing
        }
    }
}

private fun generateShareText(state: ReceiptUiState.Success): String {
    val sb = StringBuilder()
    sb.append("BUDGETWISE RECEIPT\n")
    sb.append("Date: ${DateUtils.getTodayDateStr()}\n")
    sb.append("------------------------\n")
    state.transactions.forEach {
        val amountStr = "${if (it.type == "income") "+" else "-"}${it.amount.toINR()}"
        sb.append("${it.merchant ?: it.category}: $amountStr\n")
    }
    sb.append("------------------------\n")
    sb.append("TOTAL SPENT: ${state.totalExpenses.toINR()}\n")
    return sb.toString()
}
