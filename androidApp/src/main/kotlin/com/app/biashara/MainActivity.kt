package com.app.biashara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.app.biashara.ui.Biashara360App
import com.app.biashara.ui.theme.Biashara360Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Biashara360Theme {
                Biashara360App()
            }
        }
    }
}
