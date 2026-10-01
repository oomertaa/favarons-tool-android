package ro.favaron.costuri.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ro.favaron.costuri.bd
import java.time.LocalDate

class FmtTest {

    @Test
    fun adaptive_examples_from_spec() {
        assertEquals("12,50", Fmt.adaptive(bd("12.5")))
        assertEquals("0,0125", Fmt.adaptive(bd("0.0125")))
        assertEquals("0,001", Fmt.adaptive(bd("0.001")))
        assertEquals("0,50", Fmt.adaptive(bd("0.5")))
    }

    @Test
    fun adaptive_min_2_max_4_decimals() {
        assertEquals("0,00", Fmt.adaptive(bd("0")))
        assertEquals("100,00", Fmt.adaptive(bd("100")))
        assertEquals("100,00", Fmt.adaptive(bd("1E+2")))
        assertEquals("0,0125", Fmt.adaptive(bd("0.0125000000")))
        assertEquals("0,04", Fmt.adaptive(bd("0.03996"))) // 4 decimals max: 0,0400 -> 0,04
        assertEquals("1,2346", Fmt.adaptive(bd("1.23456789")))
        assertEquals("0,333", Fmt.adaptive(bd("0.333")))
        assertEquals("3,3333", Fmt.adaptive(bd("3.3333333333")))
        assertEquals("0,40", Fmt.adaptive(bd("0.4000000000")))
    }

    @Test
    fun tiny_non_zero_values_are_never_shown_as_zero() {
        assertEquals("0,00004", Fmt.adaptive(bd("0.00004")))
        assertEquals("0,000001", Fmt.adaptive(bd("0.00000123")))
        assertEquals("0,0000004", Fmt.adaptive(bd("0.0000004")))
        assertEquals("0,000000001", Fmt.adaptive(bd("1E-9")))
        listOf("0.00004999", "0.000005", "0.0000000123", "0.00000000001").forEach {
            val s = Fmt.adaptive(bd(it))
            assertNotEquals("value $it shown as zero", 0, bd(s.replace(',', '.')).signum())
        }
    }

    @Test
    fun totals_fixed_two_decimals_half_up() {
        assertEquals("1,01", Fmt.money(bd("1.005")))
        assertEquals("1,00", Fmt.money(bd("1.0049999")))
        assertEquals("30,19", Fmt.money(bd("30.192")))
        assertEquals("0,00", Fmt.money(bd("0.004")))
        assertEquals("-2,50", Fmt.money(bd("-2.5")))
        assertEquals("1234567,89", Fmt.money(bd("1234567.891"))) // no thousands separator
    }

    @Test
    fun percent_one_decimal() {
        assertEquals("87,9", Fmt.percent(bd("87.923")))
        assertEquals("50,0", Fmt.percent(bd("50")))
    }

    @Test
    fun quantities_trimmed_with_comma() {
        assertEquals("120", Fmt.qty(bd("120.0000000000")))
        assertEquals("100", Fmt.qty(bd("1E+2")))
        assertEquals("1,5", Fmt.qty(bd("1.50")))
        assertEquals("33,3333", Fmt.qty(bd("33.3333333333")))
        assertEquals("0", Fmt.qty(bd("0.000")))
        assertEquals("0,00001", Fmt.qty(bd("0.00001")))
    }

    @Test
    fun date_format() {
        assertEquals("05.03.2026", Fmt.date(LocalDate.of(2026, 3, 5)))
    }

    @Test
    fun parsing_accepts_decimal_comma_and_rejects_bad_input() {
        assertEquals(bd("12.5"), Num.parse("12,5"))
        assertEquals(bd("12.5"), Num.parse("12.5"))
        assertEquals(bd("0.25"), Num.parse(",25"))
        assertEquals(bd("12"), Num.parse("12,"))
        assertEquals(bd("0.0001234"), Num.parse(" 0,0001234 "))
        assertNull(Num.parse("-1"))
        assertNull(Num.parse("1.000,50"))
        assertNull(Num.parse("1 000"))
        assertNull(Num.parse("abc"))
        assertNull(Num.parse(","))
        assertNull(Num.parse(""))
    }

    @Test
    fun validation_rules() {
        assertEquals(bd("12.50"), Validate.money("12,50").first)
        assertEquals("Maxim 2 zecimale", Validate.money("12,505").second)
        assertEquals(bd("12.500"), Validate.money("12,500").first) // trailing zeros are fine
        assertEquals("Trebuie > 0", Validate.positiveQuantity("0").second) // no division by zero
        assertEquals("Număr invalid", Validate.positiveQuantity("-5").second)
        assertEquals(bd("0.000125"), Validate.positiveQuantity("0,000125").first)
        assertEquals("Minim 1", Validate.wholeNumber("0").second) // yield >= 1
        assertEquals("Număr întreg invalid", Validate.wholeNumber("2,5").second)
        assertEquals(12, Validate.wholeNumber("12").first)
        assertNull(Validate.money("", required = false).second)
    }
}
