package com.pennywiseai.tracker.domain.model

/**
 * Data class representing SpendSense dashboard results
 * Combines current and last month data with calculated differences
 */
data class SpendSenseResult(
    val thisMonth: MonthlySummary,
    val lastMonth: MonthlySummary,
    val diffIncome: Double,
    val diffExpense: Double,
    val diffSavings: Double
) {
    /**
     * Calculate income change percentage
     */
    val incomeChangePercentage: Double
        get() = if (lastMonth.income > 0) {
            (diffIncome / lastMonth.income * 100)
        } else {
            0.0
        }

    /**
     * Calculate expense change percentage
     */
    val expenseChangePercentage: Double
        get() = if (lastMonth.expense > 0) {
            (diffExpense / lastMonth.expense * 100)
        } else {
            0.0
        }

    /**
     * Calculate savings rate for current month
     */
    val savingsRate: Double
        get() = if (thisMonth.income > 0) {
            (thisMonth.savings / thisMonth.income * 100)
        } else {
            0.0
        }

    /**
     * Check if overall financial performance improved
     */
    val isImproved: Boolean
        get() = diffSavings >= 0

    companion object {
        /**
         * Create empty SpendSenseResult for initial state
         */
        fun empty(): SpendSenseResult {
            val emptySummary = MonthlySummary(0.0, 0.0, 0.0)
            return SpendSenseResult(
                thisMonth = emptySummary,
                lastMonth = emptySummary,
                diffIncome = 0.0,
                diffExpense = 0.0,
                diffSavings = 0.0
            )
        }
    }
}

