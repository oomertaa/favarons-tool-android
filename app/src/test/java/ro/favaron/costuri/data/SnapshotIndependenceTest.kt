package ro.favaron.costuri.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ro.favaron.costuri.assertBd
import ro.favaron.costuri.bd
import ro.favaron.costuri.domain.Csv
import ro.favaron.costuri.domain.Order
import ro.favaron.costuri.domain.OrderEditor
import ro.favaron.costuri.domain.OrderFactory
import ro.favaron.costuri.domain.RecipeView
import ro.favaron.costuri.domain.Summaries
import ro.favaron.costuri.domain.buildRecipeViews
import java.time.LocalDate

/**
 * Mandatory independence tests: after an order is saved, NO change to live data
 * (ingredients, recipes) may change any stored history value, the summaries
 * or the CSV export.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SnapshotIndependenceTest {
    private lateinit var db: AppDatabase
    private lateinit var live: LiveRepository
    private lateinit var history: HistoryRepository

    private var flourId = 0L
    private var butterId = 0L
    private var boxId = 0L
    private var cupId = 0L
    private var cakeId = 0L
    private var orderId = 0L

    @Before
    fun setUp() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        live = LiveRepository(db)
        history = HistoryRepository(db)

        flourId = live.saveIngredient(IngredientEntity(0, "Făină", "g", bd("1000"), bd("12.50"), false))
        butterId = live.saveIngredient(IngredientEntity(0, "Unt", "g", bd("250"), bd("9.99"), false))
        boxId = live.saveIngredient(IngredientEntity(0, "Cutie", "buc", bd("10"), bd("15.00"), true))
        cupId = live.saveIngredient(IngredientEntity(0, "Pahar", "buc", bd("50"), bd("20.00"), true))
        cakeId = live.saveRecipe(
            RecipeEntity(0, "Tort", 10, bd("25.00")),
            listOf(
                line(flourId, "Făină", "g", "400"),
                line(butterId, "Unt", "g", "200"),
                line(boxId, "Cutie", "buc", "10"),
                line(cupId, "Pahar", "buc", "4", fixed = true),
            ),
        )
        orderId = history.save(
            Order(
                date = LocalDate.of(2026, 9, 30),
                customer = "Ana",
                note = "ridicare la 17",
                products = listOf(OrderFactory.buildProduct(cake(), 3, bd("24.99"))),
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    private fun line(id: Long, name: String, unit: String, qty: String, fixed: Boolean = false) =
        RecipeLineEntity(0, 0, 0, id, name, unit, bd(qty), fixed)

    private suspend fun recipes(): List<RecipeView> = buildRecipeViews(live.getRecipes(), live.getIngredients())
    private suspend fun cake(): RecipeView = recipes().single { it.recipe.id == cakeId }

    /** Everything observable about the history: raw rows, domain, summary, CSV. */
    private data class HistoryState(
        val orders: List<OrderEntity>,
        val products: List<OrderProductEntity>,
        val lines: List<OrderIngredientLineEntity>,
        val domain: List<Order>,
        val summary: ro.favaron.costuri.domain.Summary,
        val csv: String,
    )

    private suspend fun state(): HistoryState {
        val dao = db.historyDao()
        val domain = history.loadAll()
        return HistoryState(dao.getOrders(), dao.getProducts(), dao.getLines(), domain, Summaries.summarize(domain), Csv.sales(domain))
    }

    private fun assertUnchanged(change: suspend () -> Unit) = runBlocking {
        val before = state()
        change()
        val after = state()
        assertEquals(before.orders, after.orders)
        assertEquals(before.products, after.products)
        assertEquals(before.lines, after.lines)
        assertEquals(before.domain, after.domain)
        assertEquals(before.summary, after.summary)
        assertEquals(before.csv, after.csv)
        // and the flow the UI observes agrees
        assertEquals(before.domain, history.orders.first())
    }

    @Test
    fun saved_snapshot_contains_plain_values() = runBlocking {
        val o = history.loadAll().single()
        val p = o.products.single()
        assertEquals("Tort", p.name)
        assertEquals(3, p.pieces)
        assertEquals(10, p.recipeYield)
        assertBd("24.99", p.pricePerPiece)
        val flour = p.lines.first { it.name == "Făină" }
        assertBd("120", flour.quantity)
        assertBd("40", flour.basePerPiece)
        assertEquals(bd("0.0125000000"), flour.unitPrice) // stored at full precision
        assertBd("1.5", flour.lineCost)
        val cup = p.lines.first { it.name == "Pahar" }
        assertTrue(cup.fixedQuantity)
        assertBd("4", cup.quantity)
        val row = db.historyDao().getOrders().single()
        assertBd("74.97", row.totalRevenue)
        assertBd(o.cost.toPlainString(), row.totalCost)
        assertFalse(row.manuallyEdited)
    }

    @Test
    fun changing_ingredient_price_does_not_change_history() = assertUnchanged {
        val f = live.getIngredients().first { it.id == flourId }
        live.saveIngredient(f.copy(packagePrice = bd("99.99")))
        assertNotEquals(bd("0.0125000000"), cake().lines.first().unitPrice) // live did change
    }

    @Test
    fun changing_ingredient_package_quantity_does_not_change_history() = assertUnchanged {
        val f = live.getIngredients().first { it.id == flourId }
        live.saveIngredient(f.copy(packageQuantity = bd("500")))
    }

    @Test
    fun renaming_ingredient_does_not_change_history() = assertUnchanged {
        val f = live.getIngredients().first { it.id == flourId }
        live.saveIngredient(f.copy(name = "Făină 000", unit = "ml"))
        assertEquals("Făină 000", cake().lines.first().name)
    }

    @Test
    fun deleting_ingredient_does_not_change_history() = assertUnchanged {
        live.deleteIngredient(flourId)
        assertTrue(cake().hasMissing)
    }

    @Test
    fun changing_recipe_lines_yield_and_price_does_not_change_history() = assertUnchanged {
        live.saveRecipe(
            RecipeEntity(cakeId, "Tort de ciocolată", 4, bd("80.00")),
            listOf(line(butterId, "Unt", "g", "1000"), line(boxId, "Cutie", "buc", "1", fixed = true)),
        )
        assertEquals(4, cake().recipe.yieldPieces)
    }

    @Test
    fun scaling_recipe_does_not_change_history() = assertUnchanged {
        live.scaleRecipe(cakeId, 20)
    }

    @Test
    fun scale_button_multiplies_quantities_and_costs_but_plain_yield_edit_does_not() = runBlocking {
        val before = cake()
        assertBd("29.592", before.batchCost) // 5 + 7.992 + 15 + 1.6
        live.scaleRecipe(cakeId, 15)
        val s = cake()
        assertEquals(15, s.recipe.yieldPieces)
        val q = s.lines.associate { it.name to it.line.quantity }
        assertBd("600", q["Făină"]!!)
        assertBd("300", q["Unt"]!!)
        assertBd("15", q["Cutie"]!!) // consumable, scaled
        assertBd("4", q["Pahar"]!!) // fixed consumable unchanged
        assertBd("43.588", s.batchCost) // 7.5 + 11.988 + 22.5 + 1.6
        assertBd(before.recipe.sellingPrice!!.toPlainString(), s.recipe.sellingPrice!!) // price per piece unchanged

        // a plain edit of the yield is only a correction: quantities stay
        live.saveRecipe(s.recipe.copy(yieldPieces = 30), s.lines.map { it.line })
        val c = cake()
        assertEquals(30, c.recipe.yieldPieces)
        assertEquals(s.lines.map { it.line.quantity }, c.lines.map { it.line.quantity })
        assertEquals(s.batchCost, c.batchCost)
    }

    @Test
    fun deleting_recipe_does_not_change_history() = assertUnchanged {
        live.deleteRecipe(cakeId)
        assertTrue(recipes().isEmpty())
    }

    @Test
    fun deleting_everything_live_does_not_change_history() = assertUnchanged {
        live.deleteRecipe(cakeId)
        live.getIngredients().forEach { live.deleteIngredient(it.id) }
    }

    @Test
    fun manual_edits_persist_and_do_not_touch_live_data() = runBlocking {
        val liveIngredientsBefore = live.getIngredients()
        val liveRecipesBefore = live.getRecipes()
        var o = history.loadAll().single()
        val p = o.products.single()
        val flour = p.lines.first { it.name == "Făină" }
        o = OrderEditor.setLineName(o, p.key, flour.key, "Făină T650")
        o = OrderEditor.setQuantity(o, p.key, flour.key, bd("130"))
        o = OrderEditor.setCustomer(o, "Ana Pop")
        o = OrderEditor.setProductName(o, p.key, "Tort aniversar")
        history.save(o)

        val saved = history.loadAll().single()
        assertTrue(saved.manuallyEdited)
        assertEquals("Ana Pop", saved.customer)
        assertEquals("Tort aniversar", saved.products.single().name)
        val l = saved.products.single().lines.first { it.name == "Făină T650" }
        assertBd("130", l.quantity)
        assertBd("1.625", l.lineCost)
        assertEquals(liveIngredientsBefore, live.getIngredients())
        assertEquals(liveRecipesBefore, live.getRecipes())
        assertTrue(Csv.sales(listOf(saved)).contains("Făină T650;g;Nu;130;0,0125;1,625;24,99"))
    }

    @Test
    fun recalculation_uses_only_stored_values_even_if_live_changed_or_deleted() = runBlocking {
        // live price changes and the ingredient is then deleted
        val f = live.getIngredients().first { it.id == flourId }
        live.saveIngredient(f.copy(packagePrice = bd("50.00")))
        live.deleteIngredient(flourId)
        live.deleteRecipe(cakeId)

        var o = history.loadAll().single()
        val p = o.products.single()
        val flour = p.lines.first { it.name == "Făină" }
        o = OrderEditor.setQuantity(o, p.key, flour.key, bd("200"))
        assertBd("2.5", o.products.single().lines.first { it.name == "Făină" }.lineCost) // stored 0.0125
        o = OrderEditor.rescale(o, p.key, 6)
        val r = o.products.single()
        assertBd("240", r.lines.first { it.name == "Făină" }.quantity)
        assertBd("3", r.lines.first { it.name == "Făină" }.lineCost)
        assertBd("6", r.lines.first { it.name == "Cutie" }.quantity)
        assertBd("4", r.lines.first { it.name == "Pahar" }.quantity) // fixed
        history.save(o)
        val saved = history.loadAll().single()
        assertEquals(o.copy(manuallyEdited = true).products.map { it.lines.map { l -> l.copy(key = 0) } },
            saved.products.map { it.lines.map { l -> l.copy(key = 0) } })
        assertEquals(6, saved.products.single().pieces)
    }

    @Test
    fun editing_pieces_sold_changes_revenue_but_not_costs() = runBlocking {
        val o = history.loadAll().single()
        val p = o.products.single()
        history.save(OrderEditor.setPieces(o, p.key, 10))
        val saved = history.loadAll().single()
        assertBd("249.9", saved.revenue)
        assertEquals(o.cost, saved.cost)
        assertEquals(p.lines.map { it.copy(key = 0) }, saved.products.single().lines.map { it.copy(key = 0) })
        assertBd("249.9", db.historyDao().getOrders().single().totalRevenue)
        assertBd("249.9", Summaries.summarize(listOf(saved)).revenue)
    }

    @Test
    fun adding_and_deleting_manual_lines_updates_stored_totals() = runBlocking {
        var o = history.loadAll().single()
        val p = o.products.single()
        val costBefore = o.cost
        o = OrderEditor.addLine(o, p.key, -1, "Panglică", "m", true, bd("2"), bd("1.25"))
        history.save(o)
        var saved = history.loadAll().single()
        assertBd(costBefore.add(bd("2.5")).toPlainString(), saved.cost)
        assertBd(costBefore.add(bd("2.5")).toPlainString(), db.historyDao().getOrders().single().totalCost)
        assertBd(costBefore.add(bd("2.5")).toPlainString(), db.historyDao().getProducts().single().productCost)

        val sp = saved.products.single()
        saved = OrderEditor.deleteLine(saved, sp.key, sp.lines.first { it.name == "Panglică" }.key)
        saved = OrderEditor.deleteLine(saved, sp.key, sp.lines.first { it.name == "Făină" }.key)
        history.save(saved)
        val again = history.loadAll().single()
        assertBd(costBefore.subtract(bd("1.5")).toPlainString(), again.cost)
        assertBd(costBefore.subtract(bd("1.5")).toPlainString(), db.historyDao().getOrders().single().totalCost)
        assertEquals(3, db.historyDao().getLines().size)
    }

    @Test
    fun deleting_used_ingredient_is_allowed_with_warning_and_blocks_new_orders_until_fixed() = runBlocking {
        // warning lists affected recipes
        assertEquals(listOf("Tort"), live.recipesUsing(butterId))
        live.deleteIngredient(butterId)
        assertTrue(live.getIngredients().none { it.id == butterId })

        val broken = cake()
        assertTrue(broken.hasMissing)
        assertFalse(broken.canBeOrdered)
        val missing = broken.lines.single { it.missing }
        assertEquals("Unt", missing.name) // stored name kept
        assertBd("200", missing.line.quantity) // stored quantity kept
        try {
            OrderFactory.buildProduct(broken, 1, bd("25"))
            org.junit.Assert.fail("Recipe with missing ingredient must not be orderable")
        } catch (e: OrderFactory.MissingIngredientException) {
            // expected
        }

        // replace the missing line with an existing ingredient -> orderable again
        val newButter = live.saveIngredient(IngredientEntity(0, "Unt 82%", "g", bd("200"), bd("11.00"), false))
        live.saveRecipe(
            broken.recipe,
            broken.lines.map { lv ->
                if (lv.missing) lv.line.copy(ingredientId = newButter, ingredientName = "Unt 82%") else lv.line
            },
        )
        assertTrue(cake().canBeOrdered)
        OrderFactory.buildProduct(cake(), 1, bd("25"))

        // removing the missing line instead also works
        live.deleteIngredient(newButter)
        val b2 = cake()
        assertFalse(b2.canBeOrdered)
        live.saveRecipe(b2.recipe, b2.lines.filterNot { it.missing }.map { it.line })
        assertTrue(cake().canBeOrdered)
    }

    @Test
    fun deleting_an_order_removes_all_its_rows() = runBlocking {
        val second = history.save(
            Order(date = LocalDate.of(2026, 10, 1), products = listOf(OrderFactory.buildProduct(cake(), 1, bd("25")))),
        )
        history.delete(orderId)
        val dao = db.historyDao()
        assertEquals(listOf(second), dao.getOrders().map { it.id })
        assertTrue(dao.getProducts().all { it.orderId == second })
        assertTrue(dao.getLines().all { it.orderId == second })
    }
}
