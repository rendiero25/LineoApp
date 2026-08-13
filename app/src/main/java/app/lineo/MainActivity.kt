package app.lineo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * Single activity host. Screens are navigation destinations; nothing else lives here.
 *
 * The content is intentionally empty until `:core:ui` provides the theme and the
 * expression editor (P0-12, P0-14).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { }
    }
}
