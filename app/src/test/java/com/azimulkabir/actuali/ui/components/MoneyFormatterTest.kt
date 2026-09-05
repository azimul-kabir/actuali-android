package com.azimulkabir.actuali.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatterTest {
    @Test fun formatsExactCents() {
        assertEquals("৳1,234.56", formatMoneyCents(123456, hideDecimalPlaces = false))
        assertEquals("−৳1,234.56", formatMoneyCents(-123456, hideDecimalPlaces = false))
        assertEquals("+৳1,234.56", formatMoneyCents(123456, false, showPositiveSign = true))
    }

    @Test fun hidingDecimalsOnlyChangesPresentation() {
        assertEquals("৳1,234", formatMoneyCents(123456, hideDecimalPlaces = true))
        assertEquals("1234.56", centsToInput(123456))
        assertEquals(123456L, parseInputCents("1234.56"))
    }

    @Test fun inputRequiresAtMostExactCents() {
        assertEquals(120L, parseInputCents("1.20"))
        assertNull(parseInputCents("1.234"))
        assertNull(parseInputCents("not money"))
    }
}
