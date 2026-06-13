package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.*
import java.math.BigDecimal

class AirtelMoneyKenyaParserTest {
    @TestFactory
    fun `test Airtel Money Kenya Parser comprehensive test suite`(): List<DynamicTest> {
        val parser = AirtelMoneyKenyaParser()

        ParserTestUtils.printTestHeader(
            parserName = "Airtel Money Kenya",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "Airtime Purchase via Airtel",
                message = """AB1234XY Confirmed. You have successfully purchased Airtime worth Ksh50 for your line via Airtel Kenya on 12/01/24 at 10:00 AM. Fee: Ksh1. Bal: Ksh1,450.00.""",
                sender = "AIRTELMONEY",
                expected = ExpectedTransaction(
                    amount = BigDecimal("50"),
                    currency = "KES",
                    type = TransactionType.EXPENSE,
                    merchant = "Airtel Kenya",
                    balance = BigDecimal("1450.00"),
                    reference = "AB1234XY"
                )
            ),
            ParserTestCase(
                name = "Payment to Merchant",
                message = """CD5678ZA Confirmed. Ksh2000 successfully paid to Naivas Supermarket on 12/01/24 at 14:32 PM. Transaction cost Ksh30. Bal: Ksh8,500.00.""",
                sender = "AIRTELMONEY",
                expected = ExpectedTransaction(
                    amount = BigDecimal("2000"),
                    currency = "KES",
                    type = TransactionType.EXPENSE,
                    merchant = "Naivas Supermarket",
                    balance = BigDecimal("8500.00"),
                    reference = "CD5678ZA"
                )
            ),
            ParserTestCase(
                name = "Received from MPESA with TID Prefix",
                message = """TID: EF9012BC. Received Ksh1500 from John Doe on 12/01/24 at 09:00 AM. Bal:Ksh3,500.00 Sender TID: GH3456DE.""",
                sender = "AIRTELMONEY",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1500"),
                    currency = "KES",
                    type = TransactionType.INCOME,
                    merchant = "John Doe",
                    balance = BigDecimal("3500.00"),
                    reference = "EF9012BC"
                )
            ),
            ParserTestCase(
                name = "You Have Received from MPESA Sender",
                message = """You have received Ksh750.00 from MPESA Jane Smith . TID: IJ7890FG. Fee: Ksh0. Bal:Ksh4250.00.""",
                sender = "AIRTELMONEY",
                expected = ExpectedTransaction(
                    amount = BigDecimal("750.00"),
                    currency = "KES",
                    type = TransactionType.INCOME,
                    merchant = "Jane Smith",
                    balance = BigDecimal("4250.00"),
                    reference = "IJ7890FG"
                )
            ),
        )

        val handleCases: List<Pair<String, Boolean>> = listOf(
            "AIRTELMONEY" to true,
            "airtelmoney" to true,
            "AIRTEL-MONEY" to true,
            "AIRBNK" to false,
            "MPESA" to false,
            "HDFC" to false,
            "" to false
        )

        return ParserTestUtils.runTestSuite(parser, testCases, handleCases, "Airtel Money Kenya Parser Tests")
    }
}
