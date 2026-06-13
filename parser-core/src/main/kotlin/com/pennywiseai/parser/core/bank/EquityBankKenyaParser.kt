package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal

class EquityBankKenyaParser : BankParser() {

    companion object {
        // These SMS confirm Bank->MPESA transfers and never include the user's own
        // Equity account number, so the account is keyed by a stable identifier so
        // it is still auto-detected rather than left undetected.
        const val WALLET_ACCOUNT = "WALLET"
    }

    override fun getBankName() = "Equity Bank Kenya"

    override fun getCurrency() = "KES"

    override fun canHandle(sender: String): Boolean {
        return sender.uppercase().contains("EQUITY")
    }

    override fun extractAmount(message: String): BigDecimal? {
        // "Your transaction of Kshs. 1000.00 has been credited to..."
        val pattern = Regex(
            """transaction\s+of\s+Kshs?\.?\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+has\s+been\s+credited""",
            RegexOption.IGNORE_CASE
        )
        pattern.find(message)?.let { match ->
            return runCatching { BigDecimal(match.groupValues[1].replace(",", "")) }.getOrNull()
        }
        return null
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        return if (lower.contains("transaction of kshs") && lower.contains("has been credited to")) {
            TransactionType.EXPENSE
        } else {
            null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // "credited to 254758802013 MERCHANT_NAME. Ref."
        Regex("""credited\s+to\s+\d+\s+(.+?)\s*\.\s*Ref""", RegexOption.IGNORE_CASE)
            .find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) return merchant
            }
        return null
    }

    override fun extractAccountLast4(message: String): String = WALLET_ACCOUNT

    override fun extractBalance(message: String): BigDecimal? = null

    override fun extractReference(message: String): String? {
        // "Ref. TID123. MPESA Ref. Y" — first Ref is the Equity Bank transaction ref
        Regex("""Ref\.?\s*(\w+)\.\s*MPESA\s+Ref""", RegexOption.IGNORE_CASE).find(message)?.let {
            return it.groupValues[1]
        }
        // Fallback: "Ref. TID."
        Regex("""Ref\.?\s*(\w+)\.""", RegexOption.IGNORE_CASE).find(message)?.let {
            return it.groupValues[1]
        }
        return null
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("transaction of kshs") && lower.contains("credited to")
    }
}
