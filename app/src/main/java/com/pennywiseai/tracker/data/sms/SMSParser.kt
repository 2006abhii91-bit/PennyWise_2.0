package com.pennywiseai.tracker.data.sms


/**
 * Transaction type enum for SpendSense
 */
enum class TransactionType {
    INCOME,
    EXPENSE
}

/**
 * Parsed transaction from SMS
 */
data class ParsedTransaction(
    val type: TransactionType,
    val amount: Double,
    val timestamp: Long
)

/**
 * SMS Parser for SpendSense feature
 * Extracts transaction amounts and classifies them as income or expense
 */
class SMSParser {
    
    private val amountRegex = Regex("(?i)(rs|inr|₹)[\\s.:]*([0-9]+(?:\\.[0-9]+)?)")
    
    private val incomeKeywords = listOf("credited", "received", "deposit", "salary", "bonus", "interest", "refund")
    private val expenseKeywords = listOf("debited", "spent", "payment", "withdrawn", "purchase", "transaction", "transfer")

    /**
     * Parse SMS message and extract transaction information
     * @param messageBody SMS message text
     * @param timestamp Message timestamp
     * @return ParsedTransaction or null if parsing fails
     */
    fun parse(messageBody: String, timestamp: Long): ParsedTransaction? {
        val text = messageBody.lowercase()

        // Extract amount using regex pattern: Rs/INR/₹ followed by any number
        val match = amountRegex.find(messageBody) ?: return null
        val amount = match.groupValues[2].toDoubleOrNull() ?: return null
        
        if (amount <= 0) return null

        // Classify transaction type using keyword rules
        val type = when {
            incomeKeywords.any { text.contains(it) } -> TransactionType.INCOME
            expenseKeywords.any { text.contains(it) } -> TransactionType.EXPENSE
            else -> return null // Skip messages that don't clearly indicate income or expense
        }

        return ParsedTransaction(type, amount, timestamp)
    }
}
