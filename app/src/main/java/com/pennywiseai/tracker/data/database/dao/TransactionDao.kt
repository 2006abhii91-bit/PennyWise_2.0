package com.pennywiseai.tracker.data.database.dao

import androidx.room.*
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Int): TransactionEntity?
    
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE month = :month AND year = :year
        ORDER BY timestamp DESC
    """)
    suspend fun getTransactionsForMonth(month: Int, year: Int): List<TransactionEntity>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE month = :month AND year = :year
        ORDER BY timestamp DESC
    """)
    fun getTransactionsForMonthFlow(month: Int, year: Int): Flow<List<TransactionEntity>>
    
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type IN ('CREDIT', 'INCOME')
        AND month = :month AND year = :year
    """)
    suspend fun getMonthlyIncome(month: Int, year: Int): Double?

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type IN ('DEBIT', 'EXPENSE')
        AND month = :month AND year = :year
    """)
    suspend fun getMonthlyExpense(month: Int, year: Int): Double?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
    
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)
    
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
    
    // Method to get all transactions once (not as Flow)
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>
    
    // Method to get transactions between two dates (for SpendSense) - using timestamp comparison
    @Query("""
        SELECT * FROM transactions 
        WHERE timestamp BETWEEN :startDate AND :endDate 
        ORDER BY timestamp DESC
    """)
    suspend fun getTransactionsBetweenDatesList(startDate: Long, endDate: Long): List<TransactionEntity>
    
    // Method to get transactions between two dates as Flow
    @Query("""
        SELECT * FROM transactions 
        WHERE timestamp BETWEEN :startDate AND :endDate 
        ORDER BY timestamp DESC
    """)
    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    
    // Additional methods expected by the existing codebase
    @Query("SELECT * FROM transactions WHERE type = :transactionType ORDER BY timestamp DESC")
    fun getTransactionsFiltered(transactionType: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>
    
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = :transactionType 
        AND timestamp BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalAmountByTypeAndPeriod(
        transactionType: String,
        startDate: Long,
        endDate: Long
    ): Double?
}

