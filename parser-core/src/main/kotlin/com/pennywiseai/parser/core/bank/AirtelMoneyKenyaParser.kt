package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal

class AirtelMoneyKenyaParser : BankParser() {

    override fun getBankName() = "Airtel Money Kenya"

    override fun getCurrency() = "KES"

    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase().replace("-", "").replace(" ", "")
        return normalized.contains("AIRTELMONEY")
    }

    override fun extractAmount(message: String): BigDecimal? {
        val patterns = listOf(
            // "Ksh 50 for your line" (airtime purchase)
            Regex("""Ksh\s?([0-9,]+(?:\.[0-9]{1,2})?)\s+for\s+your\s+line""", RegexOption.IGNORE_CASE),
            // "Ksh 1000 successfully paid to"
            Regex("""Ksh\s?([0-9,]+(?:\.[0-9]{1,2})?)\s+successfully\s+paid""", RegexOption.IGNORE_CASE),
            // "Received Ksh 500 from" / "received Ksh 500 from"
            Regex("""[Rr]eceived\s+Ksh\s?([0-9,]+(?:\.[0-9]{1,2})?)"""),
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
        return when {
            lower.contains("successfully purchased") || lower.contains("successfully paid to") -> TransactionType.EXPENSE
            lower.contains("received ksh") || lower.contains("you have received") -> TransactionType.INCOME
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // "via MERCHANT on DATE" (airtime purchase: "...for your line via Merchant on DATE")
        Regex("""via\s+(.+?)\s+on\s+\d+\/""", RegexOption.IGNORE_CASE).find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) return merchant
        }
        // "paid to MERCHANT on DATE" (payment)
        Regex("""paid\s+to\s+(.+?)\s+on\s+\d+\/""", RegexOption.IGNORE_CASE).find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) return merchant
        }
        // "from SENDER . TID:" (You have received pattern)
        Regex("""from\s+(?:MPESA\s+)?(.+?)\s+\.\s*TID:""", RegexOption.IGNORE_CASE).find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) return merchant
        }
        // "Received Ksh X from SENDER on DATE" (incoming with TID prefix)
        Regex("""from\s+(.+?)\s+on\s+\d+\/""", RegexOption.IGNORE_CASE).find(message)?.let { match ->
            var merchant = match.groupValues[1].trim()
            merchant = merchant.removePrefix("MPESA ").trim()
            merchant = cleanMerchantName(merchant)
            if (isValidMerchantName(merchant)) return merchant
        }
        return null
    }

    /**
     * Airtel Money is a mobile wallet — its SMS never carry the user's own account
     * number. Any "account NUMBER" refers to a biller, so suppress the base-class
     * account extraction to avoid spawning bogus "Airtel Money ***NNNN" accounts.
     */
    override fun extractAccountLast4(message: String): String? = null

    override fun extractBalance(message: String): BigDecimal? {
        val patterns = listOf(
            Regex("""Bal:\s*Kshs?\s?([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""Bal:Kshs?\s?([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
        )
        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                return runCatching { BigDecimal(match.groupValues[1].replace(",", "")) }.getOrNull()
            }
        }
        return null
    }

    override fun extractReference(message: String): String? {
        // "TID: ABC123." at start (incoming received messages)
        Regex("""^TID:\s?(\w+)\.""", RegexOption.IGNORE_CASE).find(message)?.let {
            return it.groupValues[1]
        }
        // "TID Confirmed." at start (outgoing payments)
        Regex("""^(\w+)\s+Confirmed\.""", RegexOption.IGNORE_CASE).find(message)?.let {
            return it.groupValues[1]
        }
        // "TID: ABC123." anywhere (You have received pattern)
        Regex("""TID:\s?(\w+)\.""", RegexOption.IGNORE_CASE).find(message)?.let {
            return it.groupValues[1]
        }
        return null
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()
        val hasConfirmation = lower.contains("confirmed") ||
                lower.contains("received ksh") ||
                lower.contains("you have received")
        return hasConfirmation && lower.contains("ksh")
    }
}
