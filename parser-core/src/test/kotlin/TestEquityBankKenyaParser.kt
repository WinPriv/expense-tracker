package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.*
import java.math.BigDecimal

class EquityBankKenyaParserTest {
    @TestFactory
    fun `test Equity Bank Kenya Parser comprehensive test suite`(): List<DynamicTest> {
        val parser = EquityBankKenyaParser()

        ParserTestUtils.printTestHeader(
            parserName = "Equity Bank Kenya",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "Outbound Transfer to MPESA Recipient",
                message = """Your transaction of Kshs. 5000.00 has been credited to 254758802013 John Doe. Ref. ABC123XY. MPESA Ref. TJK6H7T3GA on 12/01/24 at 11:00 AM.""",
                sender = "EquityBnk",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5000.00"),
                    currency = "KES",
                    type = TransactionType.EXPENSE,
                    merchant = "John Doe",
                    balance = null,
                    reference = "ABC123XY"
                )
            ),
            ParserTestCase(
                name = "Non-transaction message — ignored",
                message = "Your Equity Bank account has been activated. Call 0763000000 for help.",
                sender = "EquityBnk",
                shouldParse = false
            ),
        )

        val handleCases: List<Pair<String, Boolean>> = listOf(
            "EquityBnk" to true,
            "EQUITYBNK" to true,
            "Equity Bank" to true,
            "EQUITY" to true,
            "MPESA" to false,
            "HDFC" to false,
            "" to false
        )

        return ParserTestUtils.runTestSuite(parser, testCases, handleCases, "Equity Bank Kenya Parser Tests")
    }
}
