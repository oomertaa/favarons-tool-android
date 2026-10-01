package ro.favaron.costuri.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ro.favaron.costuri.MainActivity

/**
 * Drives the real UI end to end (tablet landscape): add an ingredient, build a
 * recipe that uses it, sell it, then edit the saved order.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h800dp-land")
class UiSmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun field(label: String) = rule.onNode(hasSetTextAction() and hasText(label, substring = true))
    private fun dialogField(label: String) =
        rule.onAllNodes(hasSetTextAction() and hasText(label, substring = true) and hasAnyAncestor(isDialog())).onLast()

    @Test
    fun full_flow_ingredient_recipe_order_edit() {
        // ---- Page 1: add an ingredient ----
        rule.onNodeWithText("Ingrediente și consumabile").assertIsDisplayed()
        rule.onNodeWithText(" Adaugă").performClick()
        dialogField("Nume").performTextInput("Făină")
        dialogField("Cantitate totală pachet").performTextInput("1000")
        dialogField("Preț total pachet").performTextInput("12,50")
        rule.onNodeWithText("Preț per g: 0,0125 RON").assertIsDisplayed()
        rule.onNodeWithText("Salvează").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithText("0,0125 RON").fetchSemanticsNodes().isNotEmpty() }

        // ---- Page 2: recipe using it + a new consumable created from the editor ----
        rule.onNodeWithText("Rețete").performClick()
        rule.onNodeWithText(" Rețetă nouă").performClick()
        field("Nume produs").performTextInput("Tort")
        field("Randament").performTextReplacement("10")
        field("Preț vânzare").performTextInput("25")
        field("Adaugă linie").performTextInput("Făi")
        rule.onAllNodesWithText("Făină").onLast().performClick()
        rule.waitForIdle()
        field("Cantitate").performTextInput("400")
        rule.onNodeWithText(" Adaugă ingredient").performClick()
        dialogField("Nume").performTextInput("Cutie")
        rule.onNode(hasText("buc") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).performClick()
        dialogField("Cantitate totală pachet").performTextInput("10")
        dialogField("Preț total pachet").performTextInput("15")
        rule.onNodeWithText("Consumabil").performClick()
        rule.onAllNodesWithText("Salvează").onLast().performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithText("Cantitate fixă").fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(hasSetTextAction() and hasText("Cantitate")).onLast().performTextInput("10")
        rule.onNodeWithText("2,00 RON").assertIsDisplayed() // cost / buc = (5 + 15) / 10
        rule.onNodeWithText("Salvează").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithText("Tort").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("20,00 RON").assertIsDisplayed()

        // ---- Page 3: new order ----
        rule.onNodeWithText("Vânzări").performClick()
        rule.onNodeWithText(" Comandă nouă").performClick()
        field("Adaugă produs").performTextInput("Tor")
        rule.onAllNodesWithText("Tort").onLast().performClick()
        rule.waitForIdle()
        field("Bucăți vândute").performTextReplacement("3")
        rule.onAllNodesWithText("75,00 RON").onFirst().assertIsDisplayed() // revenue preview
        rule.onNodeWithText("Salvează comanda").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithText("Tort × 3").fetchSemanticsNodes().isNotEmpty() }

        // expand and check the breakdown, then edit pieces (revenue only)
        rule.onNodeWithText("Tort × 3").performClick()
        rule.onNodeWithText("Ingredient").assertIsDisplayed()
        rule.onAllNodesWithText("1,50").assertCountEquals(2) // flour line cost 120 g * 0.0125 and box unit price
        rule.onNodeWithText("4,50").assertIsDisplayed() // 3 boxes * 1.50
        rule.onNodeWithText(" Editează").performClick()
        field("Bucăți vândute").performTextReplacement("4")
        rule.onAllNodesWithText("100,00 RON").onFirst().assertExists() // revenue follows pieces
        rule.onNodeWithText("Salvează modificările").performScrollTo().performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithText("modificat manual").fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithText("100,00 RON").onFirst().assertIsDisplayed()
        // costs unchanged by a pieces edit: 120 g flour + 3 boxes = 1,50 + 4,50
        rule.onAllNodesWithText("6,00 RON").onFirst().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp-port")
    fun narrow_portrait_layout_navigates_all_pages_and_opens_dialogs() {
        rule.onNodeWithText("Ingrediente și consumabile").assertIsDisplayed()
        rule.onNodeWithText("Rețete").performClick()
        rule.onNodeWithText("Nicio rețetă. Apasă „Rețetă nouă”.").assertIsDisplayed()
        rule.onNodeWithText(" Rețetă nouă").performClick()
        rule.onNodeWithText("Cost total rețetă").assertExists()
        rule.onNode(androidx.compose.ui.test.hasContentDescription("Închide")).performClick()
        rule.onNodeWithText("Vânzări").performClick()
        rule.onNodeWithText(" Comandă nouă").performClick()
        rule.onNodeWithText("Salvează comanda").performClick()
        rule.onNodeWithText("Adaugă cel puțin un produs.").assertIsDisplayed() // validation
    }
}
