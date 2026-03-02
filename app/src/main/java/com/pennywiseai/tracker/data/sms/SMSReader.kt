
package com.pennywiseai.tracker.data.sms

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SMS Reader that reads all SMS using ContentResolver
 * and filters only bank-related SMS using keyword matching
 */
class SMSReader(private val context: Context) {

    companion object {
        private const val SMS_URI = "content://sms/inbox"
        
        // Bank-related keywords for filtering SMS
        private val BANK_KEYWORDS = arrayOf(
            "hdfc", "icici", "sbi", "axis", "kotak", "indusind", "idfc", "federal", "pnb",
            "bank", "banking", "credit", "debit", "upi", "imps", "neft", "rtgs", 
            "paytm", "phonepe", "gpay", "amazon pay", "wallet"
        )
    }

    /**
     * Read all SMS using ContentResolver
     * @return List of bank-related SMS messages
     */
    suspend fun readBankSmsMessages(): List<BankSmsMessage> = withContext(Dispatchers.IO) {
        val allSmsMessages = mutableListOf<BankSmsMessage>()
        
        try {
            val contentResolver: ContentResolver = context.contentResolver
            val uri = Uri.parse(SMS_URI)
            
            val projection = arrayOf(
                "_id",
                "address", 
                "body",
                "date"
            )
            
            val sortOrder = "date DESC"
            
            val cursor: Cursor? = contentResolver.query(uri, projection, null, null, sortOrder)
            
            cursor?.use { c ->
                val addressIndex = c.getColumnIndex("address")
                val bodyIndex = c.getColumnIndex("body")
                val dateIndex = c.getColumnIndex("date")
                
                while (c.moveToNext()) {
                    val address = c.getString(addressIndex) ?: continue
                    val body = c.getString(bodyIndex) ?: continue
                    val timestamp = c.getLong(dateIndex)
                    
                    // Filter only bank-related SMS using keyword matching
                    if (isBankSms(address, body)) {
                        allSmsMessages.add(
                            BankSmsMessage(
                                sender = address,
                                body = body,
                                timestamp = timestamp,
                                dateSent = timestamp
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        allSmsMessages
    }

    /**
     * Filter only bank-related SMS using keyword matching
     */
    private fun isBankSms(sender: String, body: String): Boolean {
        val message = "${sender.lowercase()} ${body.lowercase()}"
        
        // Check for bank keywords
        return BANK_KEYWORDS.any { message.contains(it) }
    }
}

/**
 * Data class representing bank SMS message
 */
data class BankSmsMessage(
    val sender: String,
    val body: String,
    val timestamp: Long,
    val dateSent: Long
)

