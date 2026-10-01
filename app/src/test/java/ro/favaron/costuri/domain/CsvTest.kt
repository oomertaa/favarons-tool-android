package ro.favaron.costuri.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.favaron.costuri.TestData
import ro.favaron.costuri.bd
import java.time.LocalDate

class CsvTest {

    private val orders: List<Order> by lazy {
        val p1 = OrderFactory.buildProduct(TestData.cake(), 3, bd("24.99"), key = 1)
        val p2 = OrderFactory.buildProduct(TestData.cake(), 1234, bd("1999.5"), key = 2)
        var o = Order(1, LocalDate.of(2026, 10, 1), "Ana; \"Cofetăria\"\nVest", "notă", false, 0, listOf(p1, p2))
        // manual edits must be reflected in the export
        o = OrderEditor.setLineCost(o, 1, p1.lines[0].key, bd("0.00004"))
        o = OrderEditor.addLine(o, 2, -50, "Panglică", "m", true, bd("1.5"), bd("0.0000123"))
        listOf(o)
    }

    private fun rows(csv: String) = csv.removePrefix(Csv.BOM).split(Csv.EOL).filter { it.isNotEmpty() }

    @Test
    fun file_starts_with_utf8_bom_and_uses_semicolons() {
        val csv = Csv.sales(orders)
        val bytes = csv.toByteArray(Charsets.UTF_8)
        assertArrayEquals(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()), bytes.copyOfRange(0, 3))
        assertEquals(Csv.SALES_HEADER.joinToString(";"), rows(csv).first())
        assertTrue(csv.contains(";"))
    }

    @Test
    fun one_row_per_ingredient_line_and_numeric_cells_match_screen_strings() {
        val csv = Csv.sales(orders)
        val data = rows(csv).drop(1)
        val o = orders.single()
        val expectedLines = o.products.flatMap { p -> p.lines.map { p to it } }
        assertEquals(expectedLines.size, data.size)

        // Rows with quoted customer: split carefully (customer contains ';', '"' and '\n').
        val parsed = parseCsv(csv.removePrefix(Csv.BOM)).drop(1)
        assertEquals(expectedLines.size, parsed.size)
        parsed.zip(expectedLines).forEach { (cells, pl) ->
            val (p, l) = pl
            val shown = HistoryDisplay.line(l)
            assertEquals(HistoryDisplay.date(o), cells[0])
            assertEquals(o.customer, cells[1])
            assertEquals(p.name, cells[2])
            assertEquals(HistoryDisplay.pieces(p), cells[3])
            assertEquals(shown.name, cells[4])
            assertEquals(shown.unit, cells[5])
            assertEquals(shown.consumable, cells[6])
            assertEquals(shown.quantity, cells[7])
            assertEquals(shown.unitPrice, cells[8])
            assertEquals(shown.lineCost, cells[9])
            assertEquals(HistoryDisplay.price(p), cells[10])
            // same values via the shared formatter directly
            assertEquals(Fmt.qty(l.quantity), cells[7])
            assertEquals(Fmt.adaptive(l.unitPrice), cells[8])
            assertEquals(Fmt.adaptive(l.lineCost), cells[9])
            assertEquals(Fmt.money(p.pricePerPiece), cells[10])
            // numeric cells: only digits and one decimal comma; no dot, spaces or currency
            listOf(3, 7, 8, 9, 10).forEach { i ->
                assertTrue("cell '${cells[i]}'", Regex("""^\d+(,\d+)?$""").matches(cells[i]))
                assertFalse(cells[i].contains("RON"))
            }
        }
    }

    @Test
    fun export_contains_displayed_values_not_raw_precision() {
        val parsed = parseCsv(Csv.sales(orders).removePrefix(Csv.BOM)).drop(1)
        val butter = parsed.first { it[4] == "Unt" }
        assertEquals("0,04", butter[8])
        assertEquals("2,3976", butter[9])
        val flour = parsed.first { it[4] == "Făină" }
        assertEquals("0,00004", flour[9]) // manual tiny override, never shown as 0
        assertEquals("24,99", flour[10])
        val big = parsed.first { it[3] == "1234" && it[4] == "Făină" }
        assertEquals("49360", big[7]) // 400/10*1234, no thousands separator
        assertEquals("1999,50", big[10])
        val ribbon = parsed.first { it[4] == "Panglică" }
        assertEquals("1,5", ribbon[7])
        assertEquals("0,00001", ribbon[8]) // first non-zero rounding (5 decimals)
    }

    @Test
    fun text_with_separators_is_quoted() {
        assertEquals("\"a;b\"", Csv.escape("a;b"))
        assertEquals("\"say \"\"hi\"\"\"", Csv.escape("say \"hi\""))
        assertEquals("\"two\nlines\"", Csv.escape("two\nlines"))
        assertEquals("plain", Csv.escape("plain"))
        assertTrue(Csv.sales(orders).contains("\"Ana; \"\"Cofetăria\"\"\nVest\""))
    }

    @Test
    fun ingredient_and_recipe_exports_use_the_same_formatter() {
        val ing = Csv.ingredients(listOf(TestData.flour, TestData.butter))
        assertTrue(ing.startsWith(Csv.BOM))
        val rows = parseCsv(ing.removePrefix(Csv.BOM))
        assertEquals(listOf("Făină", "g", "Nu", "1000", "12,50", "0,0125"), rows[1])
        val rec = parseCsv(Csv.recipes(listOf(TestData.cake())).removePrefix(Csv.BOM))
        assertEquals("30,19", rec[1][3])
        assertEquals("3,02", rec[1][4])
    }

    /** Minimal RFC-4180 parser with ';' separator, for verifying the output. */
    private fun parseCsv(s: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var i = 0
        var quoted = false
        while (i < s.length) {
            val c = s[i]
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < s.length && s[i + 1] == '"') { cell.append('"'); i++ } else quoted = false
                } else cell.append(c)
            } else when {
                c == '"' -> quoted = true
                c == ';' -> { row.add(cell.toString()); cell.clear() }
                c == '\r' && i + 1 < s.length && s[i + 1] == '\n' -> {
                    row.add(cell.toString()); cell.clear(); rows.add(row); row = mutableListOf(); i++
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
        return rows
    }
}
