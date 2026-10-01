package ro.favaron.costuri.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.hasAnyAncestor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ro.favaron.costuri.MainActivity
import java.io.File

/** Renders screenshots (only when -Dscreenshots.dir is set) to check the real layout. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun shot(name: String) {
        val dir = System.getProperty("screenshots.dir") ?: return
        File(dir).mkdirs()
        rule.waitForIdle()
        // Draw every window (activity + open dialogs) into one bitmap, like the screen shows it.
        val wmg = Class.forName("android.view.WindowManagerGlobal")
        val inst = wmg.getMethod("getInstance").invoke(null)
        val f = wmg.getDeclaredField("mViews").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val views = (f.get(inst) as List<android.view.View>).filter { it.isShown }
        val root = views.first()
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        views.forEachIndexed { i, v ->
            val loc = IntArray(2).also { v.getLocationOnScreen(it) }
            if (i > 0) canvas.drawColor(0x66000000) // dialog scrim
            canvas.save()
            canvas.translate(loc[0].toFloat(), loc[1].toFloat())
            v.draw(canvas)
            canvas.restore()
        }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun dialogField(label: String) =
        rule.onAllNodes(hasSetTextAction() and hasText(label, substring = true) and hasAnyAncestor(isDialog())).onLast()

    private fun setup() {
        rule.onNodeWithText("Vânzări").performClick()
        rule.waitForIdle()
        shot(prefix + "0_vanzari_gol")
        rule.onNodeWithText("Ingrediente").performClick()
        rule.waitForIdle()
        rule.onNodeWithText(" Adaugă").performClick()
        dialogField("Nume").performTextInput("Făină")
        dialogField("Cantitate totală pachet").performTextInput("1000")
        dialogField("Preț total pachet").performTextInput("12,50")
        rule.onNodeWithText("Salvează").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithText("0,0125 RON").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Rețete").performClick()
        rule.onNodeWithText(" Rețetă nouă").performClick()
        rule.onNode(hasSetTextAction() and hasText("Nume produs")).performTextInput("Tort de ciocolată")
        rule.onNode(hasSetTextAction() and hasText("Randament", substring = true)).performTextReplacement("10")
        rule.onNode(hasSetTextAction() and hasText("Preț vânzare", substring = true)).performTextInput("25")
        rule.onNode(hasSetTextAction() and hasText("Adaugă linie", substring = true)).performTextInput("Făi")
        rule.onAllNodesWithText("Făină").onLast().performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction() and hasText("Cantitate")).performTextInput("400")
        rule.waitForIdle()
        shot(prefix + "2_editor")
        rule.onNodeWithText("Salvează").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithText("Tort de ciocolată").fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
        shot(prefix + "1_lista_retete")
        rule.onNodeWithText(" Scalează").performClick()
        rule.waitForIdle()
        dialogField("Număr nou de bucăți").performTextReplacement("20")
        rule.waitForIdle()
        shot(prefix + "3_dialog_scalare")
        rule.onNodeWithText("Scalează").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithText("20 buc").fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
        shot(prefix + "4_dupa_scalare")
    }

    private var prefix = ""

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-hdpi")
    fun landscape() { prefix = "landscape_"; setup() }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-port-hdpi")
    fun portrait() { prefix = "portrait_"; setup() }
}
