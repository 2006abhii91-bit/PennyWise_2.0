package com.pennywiseai.tracker.ui.screens.smarthub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.domain.model.SpendSenseResult
import kotlinx.coroutines.launch

@Composable
fun SpendSenseScreen(
    onNavigateBack: () -> Unit,
    viewModel: SpendSenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    PennyWiseScaffold(
        title = "SpendSense",
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back"
                )
            }
        },

        actions = {
            IconButton(
                onClick = { viewModel.refreshData() },
                enabled = !uiState.isRefreshing
            ) {
                if (uiState.isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh data"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    // Loading State
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading SpendSense data...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                uiState.error != null -> {
                    // Error State
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refreshData() }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                
                else -> {
                    // Success State - Show Dashboard
                    uiState.spendSenseResult?.let { result ->
                        SpendSenseDashboard(
                            result = result,
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = { viewModel.refreshData() }
                        )
                    } ?: run {
                        // No data state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No data available",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.refreshData() }
                                ) {
                                    Text("Refresh Data")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendSenseDashboard(
    result: SpendSenseResult,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        item {
            Text(
                text = "Monthly Overview",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Current Month Summary
        item {
            MonthCard(
                title = "This Month",
                summary = result.thisMonth,
                isCurrentMonth = true,
                changePercentage = formatPercentage(result.diffIncome, result.thisMonth.income),
                changeAmount = formatAmountDifference(result.diffIncome)
            )
        }
        
        // Last Month Summary
        item {
            MonthCard(
                title = "Last Month",
                summary = result.lastMonth,
                isCurrentMonth = false,
                changePercentage = null,
                changeAmount = null
            )
        }
        
        // Detailed Breakdown
        item {
            Text(
                text = "Detailed Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(
            listOf(
                BreakdownItem("Income", result.thisMonth.income, result.lastMonth.income, result.diffIncome, true),
                BreakdownItem("Expenses", result.thisMonth.expense, result.lastMonth.expense, result.diffExpense, false),
                BreakdownItem("Savings", result.thisMonth.savings, result.lastMonth.savings, result.diffSavings, true)
            )
        ) { item ->
            BreakdownCard(item = item)
        }
        
        // Performance Insights
        item {
            PerformanceInsightsCard(result = result)
        }
    }
}

@Composable
private fun MonthCard(
    title: String,
    summary: com.pennywiseai.tracker.domain.model.MonthlySummary,
    isCurrentMonth: Boolean,
    changePercentage: String?,
    changeAmount: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentMonth) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (changePercentage != null && changeAmount != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = changePercentage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (changeAmount.startsWith("+")) Color(0xFF4CAF50) else Color(0xFFF44336),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = changeAmount,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (changeAmount.startsWith("+")) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem("Income", "₹${summary.income.format()}")
                SummaryItem("Expenses", "₹${summary.expense.format()}")
                SummaryItem("Savings", "₹${summary.savings.format()}")
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, amount: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BreakdownCard(item: BreakdownItem) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₹${item.current.format()}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Last: ₹${item.previous.format()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (item.diff >= 0) "+₹${item.diff.format()}" else "₹${item.diff.format()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.diff >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PerformanceInsightsCard(result: SpendSenseResult) {
    val isImproved = result.diffSavings >= 0
    val savingsRate = if (result.thisMonth.income > 0) {
        (result.thisMonth.savings / result.thisMonth.income * 100).format()
    } else {
        "0.00"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isImproved) {
                Color(0xFFE8F5E8)
            } else {
                Color(0xFFFFF3E0)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Performance Insights",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = if (isImproved) {
                    "🎉 Great job! Your savings ${formatAmountDifference(result.diffSavings)}"
                } else {
                    "📊 Your savings ${formatAmountDifference(result.diffSavings)}. Consider reviewing your expenses."
                },
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Savings Rate: $savingsRate%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class BreakdownItem(
    val label: String,
    val current: Double,
    val previous: Double,
    val diff: Double,
    val isPositiveGood: Boolean
)

private fun Double.format(): String {
    return String.format("%.2f", this)
}

private fun formatPercentage(diff: Double, current: Double): String {
    return if (current > 0) {
        val percentage = (diff / current * 100)
        if (percentage >= 0) "+${percentage.format()}%" else "${percentage.format()}%"
    } else {
        "0.00%"
    }
}

private fun formatAmountDifference(diff: Double): String {
    return if (diff >= 0) "+₹${diff.format()}" else "₹${diff.format()}"
}

