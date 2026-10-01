package ro.favaron.costuri.domain

import java.math.BigDecimal

/**
 * Parsing and validation of user-entered numbers.
 * Accepts decimal comma (Romanian) or dot; rejects negatives, signs,
 * thousands separators and anything that is not a plain decimal number.
 */
object Num {
    private val DECIMAL = Regex("""^(\d+)?([.,](\d*))?$""")

    /** Returns the parsed value, or null if [input] is not a valid non-negative decimal. */
    fun parse(input: String): BigDecimal? {
        val t = input.trim()
        if (t.isEmpty()) return null
        val m = DECIMAL.matchEntire(t) ?: return null
        val intPart = m.groupValues[1]
        val fracPart = m.groupValues[3]
        if (intPart.isEmpty() && fracPart.isEmpty()) return null
        val normalized = (intPart.ifEmpty { "0" }) + if (fracPart.isNotEmpty()) ".$fracPart" else ""
        return BigDecimal(normalized)
    }

    /** Number of significant decimals (trailing zeros ignored). */
    fun decimals(v: BigDecimal): Int = maxOf(0, v.stripTrailingZeros().scale())

    /** Parses a whole number (no decimals). */
    fun parseInt(input: String): Int? {
        val v = parse(input) ?: return null
        if (decimals(v) > 0) return null
        return try {
            v.intValueExact()
        } catch (e: ArithmeticException) {
            null
        }
    }
}

/** Validation rules shared by every form. Messages are in Romanian. */
object Validate {
    /** Money (package price, selling price): >= 0, at most 2 decimals. */
    fun money(input: String, required: Boolean = true): Pair<BigDecimal?, String?> {
        if (input.isBlank()) return null to if (required) "Obligatoriu" else null
        val v = Num.parse(input) ?: return null to "Număr invalid"
        if (Num.decimals(v) > 2) return null to "Maxim 2 zecimale"
        return v to null
    }

    /** Strictly positive quantity (package quantity, recipe line quantity). */
    fun positiveQuantity(input: String): Pair<BigDecimal?, String?> {
        if (input.isBlank()) return null to "Obligatoriu"
        val v = Num.parse(input) ?: return null to "Număr invalid"
        if (v.signum() == 0) return null to "Trebuie > 0"
        return v to null
    }

    /** Non-negative quantity or amount, any number of decimals. */
    fun nonNegative(input: String): Pair<BigDecimal?, String?> {
        if (input.isBlank()) return null to "Obligatoriu"
        val v = Num.parse(input) ?: return null to "Număr invalid"
        return v to null
    }

    /** Whole number >= [min] (yield, pieces sold). */
    fun wholeNumber(input: String, min: Int = 1): Pair<Int?, String?> {
        if (input.isBlank()) return null to "Obligatoriu"
        val v = Num.parseInt(input) ?: return null to "Număr întreg invalid"
        if (v < min) return null to "Minim $min"
        return v to null
    }
}
