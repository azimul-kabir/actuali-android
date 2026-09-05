package com.azimulkabir.actuali.ui.transactions

import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionDateFormatTest {
    @Test fun formatsActualCompactDate() {
        assertEquals("05-Sep-26", formatTransactionDate("20260905"))
    }

    @Test fun formatsIsoDate() {
        assertEquals("05-Sep-26", formatTransactionDate("2026-09-05"))
    }

    @Test fun preservesHumanFriendlyFallbacks() {
        assertEquals("Today", formatTransactionDate("Today"))
    }
}
