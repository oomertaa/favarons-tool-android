package ro.favaron.costuri.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * THE shared display formatter. Used by every screen AND by the CSV export,
 * so the exported file always contains exactly what is shown on screen.
 *
 * Rules: decimal comma, no thousands separator, no currency/unit text.
 * Rounding (HALF_UP) happens only here, never on stored values.
 */
object Fmt {
    private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /**
     * Unit prices and line costs: at least 2 decimals, up to 4 when needed,
     * trailing zeros beyond the 2nd decimal trimmed. A non-zero value that would
     * show as zero gets up to 6 decimals (and more if still needed) so it is
     * never displayed as 0.
     */
    fun adaptive(v: BigDecimal): String {
        if (v.signum() == 0) return "0,00"
        var scale = 4
        var r = v.setScale(scale, RoundingMode.HALF_UP)
        while (r.signum() == 0 && scale < 6) {
            scale++
            r = v.setScale(scale, RoundingMode.HALF_UP)
        }
        if (r.signum() == 0) {
            // Smaller than 0,0000005: show the first significant digit.
            r = v.setScale(v.scale() - v.precision() + 1, RoundingMode.HALF_UP)
        }
        r = r.stripTrailingZeros()
        if (r.scale() < 2) r = r.setScale(2)
        return comma(r.toPlainString())
    }

    /** Totals (batch cost, cost per piece, order cost, revenue, profit, prices): fixed 2 decimals. */
    fun money(v: BigDecimal): String = comma(v.setScale(2, RoundingMode.HALF_UP).toPlainString())

    /** Margin percent: 1 decimal. */
    fun percent(v: BigDecimal): String = comma(v.setScale(1, RoundingMode.HALF_UP).toPlainString())

    /**
     * Quantities: trailing zeros trimmed, at most 4 decimals on display
     * (non-zero values are never shown as 0).
     */
    fun qty(v: BigDecimal): String {
        if (v.signum() == 0) return "0"
        var r = v.setScale(4, RoundingMode.HALF_UP)
        if (r.signum() == 0) r = v.setScale(v.scale() - v.precision() + 1, RoundingMode.HALF_UP)
        val s = r.stripTrailingZeros()
        return comma((if (s.scale() < 0) s.setScale(0) else s).toPlainString())
    }

    fun int(v: Int): String = v.toString()

    fun date(d: LocalDate): String = d.format(DATE)

    fun yesNo(b: Boolean): String = if (b) "Da" else "Nu"

    /** Full-precision value for an edit field (no rounding, decimal comma). */
    fun edit(v: BigDecimal): String {
        val s = v.stripTrailingZeros()
        return comma((if (s.scale() < 0) s.setScale(0) else s).toPlainString())
    }

    private fun comma(s: String) = s.replace('.', ',')
}
