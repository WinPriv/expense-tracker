package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.ParsedTransaction
import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal

class MPESAParser : BankParser() {

    override fun getBankName() = "M-PESA"

    override fun getCurrency() = "KES"

    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("MPESA") ||
                normalizedSender.contains("M-PESA") ||
                normalizedSender == "MPESA" ||
                normalizedSender == "M-PESA"
    }

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        val base = super.parse(smsBody, sender, timestamp) ?: return null
        if (base.type != TransactionType.TRANSFER) return base
        val lower = smsBody.lowercase()
        val (fromAccount, toAccount) = when {
            lower.contains("transferred from m-shwari") -> "M-Shwari" to "M-PESA"
            lower.contains("transferred to m-shwari") -> "M-PESA" to "M-Shwari"
            lower.contains("withdraw") -> "M-PESA" to "Cash"
            lower.contains("give") && lower.contains("cash to") -> "Cash" to "M-PESA"
            else -> return base
        }
        return base.copy(fromAccount = fromAccount, toAccount = toAccount)
    }

    override fun extractAmount(message: String): BigDecimal? {
        val patterns = listOf(
            Regex("""Ksh([0-9,]+(?:\.[0-9]{1,2})?)\s+(?:paid|sent|received)""", RegexOption.IGNORE_CASE),
            Regex("""received\s+Ksh([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""Ksh\.?\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+transferred""", RegexOption.IGNORE_CASE),
            Regex("""Withdraw\.?\s+Ksh\.?\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""give\s+Ksh\.?\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+cash""", RegexOption.IGNORE_CASE),
            Regex("""Ksh\.?\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+is\s+(?:credited|debited)""", RegexOption.IGNORE_CASE),
            Regex("""bought\s+Ksh\.?\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+of""", RegexOption.IGNORE_CASE),
        )
        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                return runCatching { BigDecimal(match.groupValues[1].replace(",", "")) }.getOrNull()
            }
        }
        return null
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        if (lower.contains("transferred from m-shwari") || lower.contains("transferred to m-shwari")) {
            return TransactionType.TRANSFER
        }
        if (lower.contains("withdraw")) {
            return TransactionType.TRANSFER
        }
        if (lower.contains("give") && lower.contains("cash to")) {
            return TransactionType.TRANSFER
        }
        if (lower.contains("is credited to your m-pesa account")) {
            return TransactionType.INCOME
        }
        if (lower.contains("is debited from your m-pesa account")) {
            return TransactionType.EXPENSE
        }
        if (lower.contains("you bought ksh")) {
            return TransactionType.EXPENSE
        }
        if (lower.contains("you have received") || lower.contains("received ksh")) {
            return TransactionType.INCOME
        }
        if (lower.contains("paid to") || lower.contains("sent to")) {
            return TransactionType.EXPENSE
        }
        return null
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val lower = message.lowercase()
        if (lower.contains("m-shwari")) {
            return "M-Shwari"
        }
        if (lower.contains("reversal of transaction")) {
            return "M-PESA Reversal"
        }
        // "You bought Ksh 50 of Airtime on 12/01/24"
        Regex("""bought\s+Ksh\.?\s*[0-9,]+(?:\.[0-9]{1,2})?\s+of\s+(.+?)\s+on\s+\d+""", RegexOption.IGNORE_CASE)
            .find(message)?.let { match ->
                val item = match.groupValues[1].trim()
                if (isValidMerchantName(item)) return item
            }
        // "Withdraw. Ksh X from AGENT New M-PESA"
        Regex("""Withdraw\.?\s+Ksh\.?\s*[0-9,]+(?:\.[0-9]{1,2})?\s+from\s+(.+?)\s+New\s+M-PESA""", RegexOption.IGNORE_CASE)
            .find(message)?.let { match ->
                val agent = match.groupValues[1].trim()
                if (isValidMerchantName(agent)) return agent
            }
        // "give Ksh X cash to AGENT . New"
        Regex("""give\s+Ksh\.?\s*[0-9,]+(?:\.[0-9]{1,2})?\s+cash\s+to\s+(.+?)\s+\.?\s*New""", RegexOption.IGNORE_CASE)
            .find(message)?.let { match ->
                val agent = match.groupValues[1].trim()
                if (isValidMerchantName(agent)) return agent
            }
        // "paid to Person 4 1. on DATE" (with trailing number)
        Regex("""paid to\s+(.+?)\s+\d+\.\s+on""", RegexOption.IGNORE_CASE)
            .find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) return merchant
            }
        // "paid to MERCHANT on DATE" (general)
        Regex("""paid to\s+(.+?)\s+on\s+\d+\/""", RegexOption.IGNORE_CASE)
            .find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) return merchant
            }
        // "sent to Person 2 0711 111 111" (spaced phone number)
        Regex("""sent to\s+(.+?)\s+0\d{3}\s+\d{3}\s+\d{3}""", RegexOption.IGNORE_CASE)
            .find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) return merchant
            }
        // "sent to PAYBILL for account NUMBER"
        Regex("""sent to\s+(.+?)\s+for account""", RegexOption.IGNORE_CASE)
            .find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) return merchant
            }
        // "received Ksh X from MERCHANT on DATE"
        Regex("""received\s+(?:Ksh[0-9,]+(?:\.[0-9]{2})?\s+)?from\s+(.+?)\s+on""", RegexOption.IGNORE_CASE)
            .find(message)?.let { match ->
                var merchant = match.groupValues[1].trim()
                    .removeSuffix(".").trim()
                    .replace(Regex("""\s+0\d{10}$"""), "")
                    .replace(Regex("""\s+\d{6,}$"""), "").trim()
                merchant = cleanMerchantName(merchant)
                if (isValidMerchantName(merchant)) return merchant
            }
        // "from COMPANY NAME. on DATE"
        Regex("""from\s+([^.]+)\.\s+on""", RegexOption.IGNORE_CASE)
            .find(message)?.let { match ->
                var merchant = match.groupValues[1].trim()
                    .replace(Regex("""\s+0\d{10}$"""), "")
                merchant = cleanMerchantName(merchant)
                if (isValidMerchantName(merchant)) return merchant
            }
        return null
    }

    override fun extractBalance(message: String): BigDecimal? {
        val patterns = listOf(
            Regex("""New M-PESA balance is Ksh([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""New M-PESA account balance is Ksh([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""M-PESA balance is Ksh\.?\s*([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""New balance is Ksh([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE),
        )
        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                return runCatching { BigDecimal(match.groupValues[1].replace(",", "")) }.getOrNull()
            }
        }
        return null
    }

    override fun extractReference(message: String): String? {
        Regex("""^([A-Z0-9]{10})\s+Confirmed""", RegexOption.IGNORE_CASE).find(message)?.let {
            return it.groupValues[1]
        }
        Regex("""^([A-Z0-9]{10})\s+Confirmed\.""", RegexOption.IGNORE_CASE).find(message)?.let {
            return it.groupValues[1]
        }
        Regex("""Congratulations!\s+([A-Z0-9]{10})\s+confirmed""", RegexOption.IGNORE_CASE).find(message)?.let {
            return it.groupValues[1]
        }
        Regex("""^([A-Z0-9]{6,12})\s+confirmed""", RegexOption.IGNORE_CASE).find(message)?.let {
            return it.groupValues[1]
        }
        return null
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()
        if (lower.contains("do not have sufficient funds") ||
            lower.contains("insufficient funds in your m-pesa") ||
            lower.contains("m-pesa is unable to process") ||
            lower.contains("transaction failed") ||
            lower.contains("wrong pin") ||
            lower.contains("cancelled the transaction of ksh")) {
            return false
        }
        if (!lower.contains("confirmed")) {
            return false
        }
        val transactionKeywords = listOf(
            "paid to", "sent to", "received",
            "new m-pesa balance",
            "m-pesa balance is",
            "new m-pesa account balance",
            "transferred from m-shwari",
            "transferred to m-shwari",
            "withdraw. ksh",
            "withdraw ksh",
            "give ksh",
            "you bought ksh",
            "new balance is"
        )
        return transactionKeywords.any { lower.contains(it) }
    }

    override fun cleanMerchantName(merchant: String): String {
        return merchant
            .replace(Regex("""\s*\(.*?\)\s*$"""), "")
            .replace(Regex("""\s+Ref\s+No.*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+on\s+\d{2}.*"""), "")
            .replace(Regex("""\s+UPI.*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+at\s+\d{2}:\d{2}.*"""), "")
            .replace(Regex("""\s*-\s*$"""), "")
            .trim()
    }
}
