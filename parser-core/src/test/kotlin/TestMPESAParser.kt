package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.*
import java.math.BigDecimal

class MPESAParserTest {
    @TestFactory
    fun `test M-PESA Parser comprehensive test suite`(): List<DynamicTest> {
        val parser = MPESAParser()

        ParserTestUtils.printTestHeader(
            parserName = "M-PESA",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "Payment to Person",
                message = """TJK6H7T3GA Confirmed. Ksh70.00 paid to person 1. on 20/10/24 at 4:21 PM.New M-PESA balance is Ksh123.12. Transaction cost, Ksh0.00. Amount you can transact within the day is 499,895.00. Save frequent Tills for quick payment on M-PESA app https://bit.ly/mpesalnk""",
                sender = "MPESA",
                expected = ExpectedTransaction(
                    amount = BigDecimal("70.00"),
                    currency = "KES",
                    type = TransactionType.EXPENSE,
                    merchant = "person",
                    balance = BigDecimal("123.12"),
                    reference = "TJK6H7T3GA"
                )
            ),
            ParserTestCase(
                name = "Paybill Payment to Equity",
                message = """TJK6H7T0JT Confirmed. Ksh1000.00 sent to Equity Paybill Account for account 123123 on 20/10/25 at 4:26 PM New M-PESA balance is Ksh123.12. Transaction cost, Ksh23.00.Amount you can transact within the day is 499,795.00. Save frequent paybills for quick payment on M-PESA app https://bit.ly/mpesalnk""",
                sender = "MPESA",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1000.00"),
                    currency = "KES",
                    type = TransactionType.EXPENSE,
                    merchant = "Equity Paybill Account",
                    balance = BigDecimal("123.12"),
                    reference = "TJK6H7T0JT"
                )
            ),
            ParserTestCase(
                name = "Send to Person with Phone Number",
                message = """TJK6H7TDIJ Confirmed. Ksh50.00 sent to Person 2 0711 111 111 on 20/10/24 at 6:27 PM. New M-PESA balance is Ksh123.12. Transaction cost, Ksh0.00. Amount you can transact within the day is 499,745.00. Earn interest daily on Ziidi MMF,Dial *334#""",
                sender = "MPESA",
                expected = ExpectedTransaction(
                    amount = BigDecimal("50.00"),
                    currency = "KES",
                    type = TransactionType.EXPENSE,
                    merchant = "Person 2",
                    balance = BigDecimal("123.12"),
                    reference = "TJK6H7TDIJ"
                )
            ),
            ParserTestCase(
                name = "Payment with Comma in Amount",
                message = """TJD6H78J2L Confirmed. Ksh1,120.00 paid to Person 4 1. on 13/10/24 at 8:01 PM.New M-PESA balance is Ksh123.12. Transaction cost, Ksh0.00. Amount you can transact within the day is 498,440.00. Save frequent Tills for quick payment on M-PESA app https://bit.ly/mpesalnk""",
                sender = "MPESA",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1120.00"),
                    currency = "KES",
                    type = TransactionType.EXPENSE,
                    merchant = "Person 4",
                    balance = BigDecimal("123.12"),
                    reference = "TJD6H78J2L"
                )
            ),
            ParserTestCase(
                name = "Received from Person",
                message = """TJF987E58C Confirmed.You have received Ksh300.00 from Person 3 0712121212 on 15/10/24 at 12:16 PM  New M-PESA balance is Ksh123.12. Earn interest daily on Ziidi MMF,Dial *334#""",
                sender = "Person 3",
                expected = ExpectedTransaction(
                    amount = BigDecimal("300.00"),
                    currency = "KES",
                    type = TransactionType.INCOME,
                    merchant = "Person 3",
                    balance = BigDecimal("123.12"),
                    reference = "TJF987E58C"
                )
            ),
            ParserTestCase(
                name = "Received from Bank",
                message = """TJE6H7BG0S Confirmed.You have received Ksh3,000.00 from BANK OF BARODA KENYA LIMITED 123123 on 14/10/24 at 7:16 PM New M-PESA balance is Ksh123.12.  Separate personal and business funds through Pochi la Biashara on *334#.""",
                sender = "Bank OF Baroda Kenya Limited",
                expected = ExpectedTransaction(
                    amount = BigDecimal("3000.00"),
                    currency = "KES",
                    type = TransactionType.INCOME,
                    merchant = "BANK OF BARODA KENYA LIMITED",
                    balance = BigDecimal("123.12"),
                    reference = "TJE6H7BG0S"
                )
            ),
            ParserTestCase(
                name = "Received from B2C Service",
                message = """Congratulations! TJ56H6J1WU confirmed.You have received Ksh425.00 from LOOP B2C. on 5/10/25 at 6:34 PM.New M-PESA balance is Ksh123.11. Separate personal and business funds through Pochi la Biashara on *334#.""",
                sender = "Loop b2c",
                expected = ExpectedTransaction(
                    amount = BigDecimal("425.00"),
                    currency = "KES",
                    type = TransactionType.INCOME,
                    merchant = "LOOP B2C",
                    balance = BigDecimal("123.11"),
                    reference = "TJ56H6J1WU"
                )
            ),
            ParserTestCase(
                name = "M-Shwari Credit Transfer",
                message = """TJA1B2C3DE Confirmed. Ksh500.00 transferred from M-Shwari account on 12/01/24 at 10:30 AM. M-Shwari balance is Ksh4,500.00 . M-PESA balance is Ksh1,500.00 . Transaction cost Ksh0.00""",
                sender = "MPESA",
                expected = ExpectedTransaction(
                    amount = BigDecimal("500.00"),
                    currency = "KES",
                    type = TransactionType.TRANSFER,
                    merchant = "M-Shwari",
                    balance = BigDecimal("1500.00"),
                    reference = "TJA1B2C3DE"
                )
            ),
            ParserTestCase(
                name = "M-Shwari Debit Transfer",
                message = """TJB2C3D4EF Confirmed. Ksh1000.00 transferred to M-Shwari account on 12/01/24 at 11:00 AM. M-PESA balance is Ksh2,000.00 . New M-Shwari account balance is Ksh6,000.00. Transaction cost Ksh0.00""",
                sender = "MPESA",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1000.00"),
                    currency = "KES",
                    type = TransactionType.TRANSFER,
                    merchant = "M-Shwari",
                    balance = BigDecimal("2000.00"),
                    reference = "TJB2C3D4EF"
                )
            ),
            ParserTestCase(
                name = "Agent Cash Withdrawal",
                message = """TJC3D4E5FG Confirmed. on 12/01/24 at 09:15 AM Withdraw. Ksh5000 from Agent Store New M-PESA balance is Ksh10000 . Transaction cost, Ksh25""",
                sender = "MPESA",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5000"),
                    currency = "KES",
                    type = TransactionType.TRANSFER,
                    balance = BigDecimal("10000"),
                    reference = "TJC3D4E5FG"
                )
            ),
            ParserTestCase(
                name = "Agent Cash Deposit",
                message = """TJD4E5F6GH confirmed. On 12/01/24 at 08:00 AM give Ksh3000 cash to Agent Store . New M-PESA balance is Ksh8000.""",
                sender = "MPESA",
                expected = ExpectedTransaction(
                    amount = BigDecimal("3000"),
                    currency = "KES",
                    type = TransactionType.TRANSFER,
                    balance = BigDecimal("8000"),
                    reference = "TJD4E5F6GH"
                )
            ),
            ParserTestCase(
                name = "Reversal Credit",
                message = """TJE5F6G7HI confirmed. Reversal of transaction TJX9Y8Z7WV has been successfully reversed  on 15/01/24  and Ksh200.00 is credited to your M-PESA account. New M-PESA account balance is Ksh1200.00""",
                sender = "MPESA",
                expected = ExpectedTransaction(
                    amount = BigDecimal("200.00"),
                    currency = "KES",
                    type = TransactionType.INCOME,
                    merchant = "M-PESA Reversal",
                    balance = BigDecimal("1200.00"),
                    reference = "TJE5F6G7HI"
                )
            ),
            ParserTestCase(
                name = "Reversal Debit",
                message = """TJF6G7H8IJ confirmed. Reversal of transaction TJY0Z1A2BC has been successfully reversed  on 15/01/24  and Ksh150.00 is debited from your M-PESA account. New M-PESA account balance is Ksh850.00""",
                sender = "MPESA",
                expected = ExpectedTransaction(
                    amount = BigDecimal("150.00"),
                    currency = "KES",
                    type = TransactionType.EXPENSE,
                    merchant = "M-PESA Reversal",
                    balance = BigDecimal("850.00"),
                    reference = "TJF6G7H8IJ"
                )
            ),
            ParserTestCase(
                name = "Airtime Purchase",
                message = """TJG7H8I9JK confirmed. You bought Ksh50 of Airtime on 12/01/24 at 10:00 AM. New M-PESA balance is Ksh950.00""",
                sender = "MPESA",
                expected = ExpectedTransaction(
                    amount = BigDecimal("50"),
                    currency = "KES",
                    type = TransactionType.EXPENSE,
                    merchant = "Airtime",
                    balance = BigDecimal("950.00"),
                    reference = "TJG7H8I9JK"
                )
            ),
            ParserTestCase(
                name = "Insufficient Funds Error — ignored",
                message = "You do not have sufficient funds in your M-PESA account to complete this transaction.",
                sender = "MPESA",
                shouldParse = false
            ),
            ParserTestCase(
                name = "Transaction Failed — ignored",
                message = "Transaction failed. Please try again later.",
                sender = "MPESA",
                shouldParse = false
            ),
            ParserTestCase(
                name = "Wrong PIN — ignored",
                message = "You have entered the wrong PIN. Please try again.",
                sender = "MPESA",
                shouldParse = false
            ),
        )

        val handleCases: List<Pair<String, Boolean>> = listOf(
            "MPESA" to true,
            "M-PESA" to true,
            "mpesa" to true,
            "m-pesa" to true,
            "Person 3" to false,
            "HDFC" to false,
            "" to false
        )

        return ParserTestUtils.runTestSuite(parser, testCases, handleCases, "M-PESA Parser Tests")
    }

    @Test
    fun `paybill message with for account does not extract an account last4`() {
        val parser = MPESAParser()
        val message = """TJK6H7T0JT Confirmed. Ksh50.00 sent to KPLC PREPAID for account 1234567890 on 13/6/26 at 4:26 PM New M-PESA balance is Ksh123.12. Transaction cost, Ksh23.00."""
        val parsed = parser.parse(message, "MPESA", System.currentTimeMillis())
        Assertions.assertNotNull(parsed, "Paybill message should parse")
        Assertions.assertNull(parsed?.accountLast4, "M-PESA must not extract an account last4 from the biller account number")
        Assertions.assertEquals("KPLC PREPAID", parsed?.merchant)
    }
}
