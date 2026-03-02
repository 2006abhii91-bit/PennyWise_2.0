
package com.pennywiseai.tracker.data.repository

import android.content.Context
import android.util.Log
import com.pennywiseai.tracker.data.database.PennyWiseDatabase
import com.pennywiseai.tracker.data.database.dao.TransactionDao
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.sms.BankSmsMessage
import com.pennywiseai.tracker.data.sms.SMSParser
import com.pennywiseai.tracker.data.sms.SMSReader
import com.pennywiseai.tracker.domain.model.MonthlySummary
import com.pennywiseai.tracker.domain.model.SpendSenseResult
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for SpendSense feature operations
 * Handles SMS reading, parsing, and monthly financial calculations
 */
@Singleton
class SpendSenseRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "SpendSenseRepository"
    
    private val smsReader = SMSReader(context)
    private val smsParser = SMSParser()
    
    /**
     * Get database instance
     */
    private fun getDatabase(): PennyWiseDatabase {
        return PennyWiseDatabase.getInstance(context)
    }
    
    private fun getTransactionDao(): TransactionDao {
        return getDatabase().transactionDao()
    }
    

    /**
     * Process new SMS messages and store transactions in database
     * @return Number of new transactions processed
     */
    suspend fun processNewSmsMessages(): Int {
        return try {
            Log.d(TAG, "Starting SMS processing")
            val bankSmsMessages = smsReader.readBankSmsMessages()
            Log.d(TAG, "Found ${bankSmsMessages.size} bank SMS messages")
            
            var processedCount = 0
            
            bankSmsMessages.forEach { smsMessage ->
                try {
                    // Parse SMS using the existing SMSParser
                    val parsedTransaction = smsParser.parse(smsMessage.body, smsMessage.timestamp)
                    
                    if (parsedTransaction != null) {
                        // Check if transaction already exists by checking for similar transactions
                        val startDate = parsedTransaction.timestamp - 60000 // 1 minute before
                        val endDate = parsedTransaction.timestamp + 60000 // 1 minute after
                        
                        val existingTransactions = getTransactionDao()
                            .getTransactionsBetweenDatesList(startDate, endDate)
                        
                        val isDuplicate = existingTransactions.any { transaction ->
                            kotlin.math.abs(transaction.amount - parsedTransaction.amount) < 0.01
                        }

                        if (!isDuplicate) {
                            // Create TransactionEntity using the simplified schema
                            val transactionDateTime = java.time.LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(parsedTransaction.timestamp),
                                java.time.ZoneId.systemDefault()
                            )
                            val month = transactionDateTime.monthValue
                            val year = transactionDateTime.year

                            val transactionEntity = TransactionEntity(
                                type = parsedTransaction.type.name,
                                amount = parsedTransaction.amount,
                                timestamp = parsedTransaction.timestamp,
                                month = month,
                                year = year
                            )
                            
                            getTransactionDao().insertTransaction(transactionEntity)
                            processedCount++
                            Log.d(TAG, "Processed transaction: ${parsedTransaction.type.name} - ₹${parsedTransaction.amount}")
                        } else {
                            Log.d(TAG, "Duplicate transaction skipped")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing SMS: ${smsMessage.body}", e)
                }
            }
            
            Log.d(TAG, "SMS processing completed. Processed: $processedCount")
            processedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error during SMS processing", e)
            0
        }
    }
    
    /**
     * Get this month's summary
     */
    suspend fun getThisMonthSummary(): MonthlySummary {
        val now = LocalDateTime.now()
        val currentMonth = now.monthValue
        val currentYear = now.year
        
        return calculateMonthlyTotals(currentMonth, currentYear)
    }
    
    /**
     * Get last month's summary
     */
    suspend fun getLastMonthSummary(): MonthlySummary {
        val now = LocalDateTime.now()
        val currentMonth = now.monthValue
        val currentYear = now.year
        
        val (lastMonthNumber, lastMonthYear) = if (currentMonth == 1) {
            12 to currentYear - 1
        } else {
            currentMonth - 1 to currentYear
        }
        
        return calculateMonthlyTotals(lastMonthNumber, lastMonthYear)
    }
    

    /**
     * Calculate monthly financial summary for given month and year
     */
    private suspend fun calculateMonthlyTotals(month: Int, year: Int): MonthlySummary {
        // Use existing DAO methods
        val income = getTransactionDao().getMonthlyIncome(month, year) ?: 0.0
        val expense = getTransactionDao().getMonthlyExpense(month, year) ?: 0.0
        val savings = income - expense
        
        return MonthlySummary(
            income = income,
            expense = expense,
            savings = savings
        )
    }
    
    /**
     * Get transactions for a specific month and year
     */
    fun getTransactionsForMonth(month: Int, year: Int): Flow<List<TransactionEntity>> {
        return getTransactionDao().getTransactionsForMonthFlow(month, year)
    }
    
    /**
     * Get SpendSense results combining current and last month data
     */
    suspend fun getSpendSenseResult(): SpendSenseResult {
        val thisMonth = getThisMonthSummary()
        val lastMonth = getLastMonthSummary()
        
        return SpendSenseResult(
            thisMonth = thisMonth,
            lastMonth = lastMonth,
            diffIncome = thisMonth.income - lastMonth.income,
            diffExpense = thisMonth.expense - lastMonth.expense,
            diffSavings = thisMonth.savings - lastMonth.savings
        )
    }
    
    /**
     * Get SpendSense results as Flow for real-time updates
     */
    fun getSpendSenseResultFlow(): Flow<SpendSenseResult> {
        val currentMonth = YearMonth.now()
        val lastMonth = currentMonth.minusMonths(1)
        
        return combine(
            getTransactionsForMonth(currentMonth.monthValue, currentMonth.year),
            getTransactionsForMonth(lastMonth.monthValue, lastMonth.year)
        ) { currentMonthTransactions, lastMonthTransactions ->
            val thisMonth = calculateSummaryFromTransactions(currentMonthTransactions)
            val lastMonth = calculateSummaryFromTransactions(lastMonthTransactions)
            
            SpendSenseResult(
                thisMonth = thisMonth,
                lastMonth = lastMonth,
                diffIncome = thisMonth.income - lastMonth.income,
                diffExpense = thisMonth.expense - lastMonth.expense,
                diffSavings = thisMonth.savings - lastMonth.savings
            )
        }
    }
    


    /**
     * Calculate MonthlySummary from TransactionEntity list
     */
    private fun calculateSummaryFromTransactions(transactions: List<TransactionEntity>): MonthlySummary {
        val income = transactions
            .filter { it.type.uppercase() in listOf("CREDIT", "INCOME") }
            .sumOf { it.amount }

        val expense = transactions
            .filter { it.type.uppercase() in listOf("DEBIT", "EXPENSE") }
            .sumOf { it.amount }

        val savings = income - expense

        return MonthlySummary(income, expense, savings)
    }
    
    /**
     * Refresh SpendSense data by processing new SMS messages
     */
    suspend fun refreshSpendSenseData() {
        processNewSmsMessages()
    }
}

