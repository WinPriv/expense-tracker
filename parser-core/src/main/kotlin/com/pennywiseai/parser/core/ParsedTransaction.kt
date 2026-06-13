package com.pennywiseai.parser.core

import java.math.BigDecimal

data class ParsedTransaction(
    val amount: BigDecimal,
    val type: TransactionType,
    val merchant: String?,
    val reference: String?,
    val accountLast4: String?,
    val balance: BigDecimal?,
    val creditLimit: BigDecimal? = null,
    val smsBody: String,
    val sender: String,
    val timestamp: Long,
    val bankName: String,
    val transactionHash: String? = null,
    val isFromCard: Boolean = false,
    val currency: String = "INR",
    val fromAccount: String? = null,
    val toAccount: String? = null,
    /**
     * Some wallet SMS report balances for two of the user's own accounts in a
     * single message — e.g. an M-PESA <-> M-Shwari transfer prints both the
     * M-PESA balance (in [balance]) and the M-Shwari balance here. When set, the
     * app records a second balance row for ([bankName], [secondaryAccountLast4])
     * so both accounts stay up to date from one SMS.
     */
    val secondaryAccountLast4: String? = null,
    val secondaryBalance: BigDecimal? = null
) {
    fun generateTransactionId(): String {
        val normalizedAmount = amount.setScale(2, java.math.RoundingMode.HALF_UP)
        // Use SMS body hash for reliable deduplication across different timestamp sources
        // (BroadcastReceiver uses SC timestamp, ContentProvider uses device timestamp)
        val smsBodyHash = md5Hex(smsBody)
            .take(16) // First 16 chars of SMS body hash
        val data = "$sender|$normalizedAmount|$smsBodyHash"
        return md5Hex(data)
    }
}


