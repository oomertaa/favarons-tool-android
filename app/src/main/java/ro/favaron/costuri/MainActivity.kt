package ro.favaron.costuri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ro.favaron.costuri.ui.AppRoot
import ro.favaron.costuri.ui.theme.FavaronTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FavaronTheme {
                AppRoot()
            }
        }
    }
}
