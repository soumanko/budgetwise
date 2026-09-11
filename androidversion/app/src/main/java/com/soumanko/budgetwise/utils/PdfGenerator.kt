package com.soumanko.budgetwise.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.soumanko.budgetwise.domain.finance.toINR
import com.soumanko.budgetwise.ui.statement.StatementUiState
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    fun generateFinancialStatementPdf(context: Context, state: StatementUiState.Success): Uri? {
        val document = PdfDocument()
        
        // A4 size roughly in PostScript points: 595 x 842
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        
        val margin = 50f
        var currentY = 50f
        
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        
        fun drawTextCentered(text: String, y: Float, p: Paint) {
            canvas.drawText(text, pageInfo.pageWidth / 2f, y, p)
        }
        
        // Page 1: Summary
        drawTextCentered("BUDGETWISE", currentY, titlePaint)
        currentY += 30f
        drawTextCentered("FINANCIAL STATEMENT", currentY, titlePaint)
        
        currentY += 50f
        canvas.drawText("Statement Period: ${state.startDate} to ${state.endDate}", margin, currentY, paint)
        currentY += 20f
        canvas.drawText("Generated: ${com.soumanko.budgetwise.domain.finance.DateUtils.getTodayDateStr()}", margin, currentY, paint)
        
        currentY += 40f
        canvas.drawLine(margin, currentY, pageInfo.pageWidth - margin, currentY, paint)
        currentY += 30f
        
        canvas.drawText("SUMMARY", margin, currentY, headerPaint)
        currentY += 30f
        
        canvas.drawText("Total Income", margin, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("+${state.totalIncome.toINR()}", pageInfo.pageWidth - margin, currentY, paint)
        paint.textAlign = Paint.Align.LEFT
        currentY += 20f
        
        canvas.drawText("Total Expenses", margin, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("-${state.totalExpenses.toINR()}", pageInfo.pageWidth - margin, currentY, paint)
        paint.textAlign = Paint.Align.LEFT
        currentY += 20f
        
        canvas.drawText("Net Change", margin, currentY, headerPaint)
        val netPrefix = if (state.netChange >= java.math.BigDecimal.ZERO) "+" else ""
        headerPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$netPrefix${state.netChange.toINR()}", pageInfo.pageWidth - margin, currentY, headerPaint)
        headerPaint.textAlign = Paint.Align.LEFT
        currentY += 40f
        
        canvas.drawLine(margin, currentY, pageInfo.pageWidth - margin, currentY, paint)
        currentY += 30f
        
        canvas.drawText("TRANSACTIONS", margin, currentY, headerPaint)
        currentY += 30f
        
        // Headers for transaction table
        canvas.drawText("DATE", margin, currentY, headerPaint)
        canvas.drawText("DESCRIPTION", margin + 100f, currentY, headerPaint)
        headerPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("AMOUNT", pageInfo.pageWidth - margin, currentY, headerPaint)
        headerPaint.textAlign = Paint.Align.LEFT
        currentY += 20f
        
        for (tx in state.transactions) {
            // Check if we need a new page
            if (currentY > pageInfo.pageHeight - margin - 50f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = margin + 30f
                
                // Redraw table headers
                canvas.drawText("DATE", margin, currentY, headerPaint)
                canvas.drawText("DESCRIPTION", margin + 100f, currentY, headerPaint)
                headerPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("AMOUNT", pageInfo.pageWidth - margin, currentY, headerPaint)
                headerPaint.textAlign = Paint.Align.LEFT
                currentY += 20f
            }
            
            canvas.drawText(tx.transactionDate, margin, currentY, paint)
            val desc = (tx.merchant ?: tx.category).take(30)
            canvas.drawText(desc, margin + 100f, currentY, paint)
            
            val isIncome = tx.type == "income"
            val amtStr = "${if (isIncome) "+" else "-"}${tx.amount.toINR()}"
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(amtStr, pageInfo.pageWidth - margin, currentY, paint)
            paint.textAlign = Paint.Align.LEFT
            
            currentY += 20f
        }
        
        document.finishPage(page)
        
        // Save to file
        return try {
            val fileName = "BudgetWise_Statement_${state.startDate}_to_${state.endDate}.pdf"
            val file = File(context.cacheDir, fileName)
            
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            document.close()
            outputStream.close()
            
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            android.util.Log.e("PdfGenerator", "PDF_FILE_WRITE_FAILED: Failed to create or write PDF document", e)
            document.close()
            null
        }
    }
}
