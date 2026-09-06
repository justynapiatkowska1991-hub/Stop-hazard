package pl.stophazard.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Zatrzymujemy ewentualny stary serwis VPN po aktualizacji.
        // STOP HAZARD nie używa VPN do blokowania stron, więc Internet pozostaje dostępny.
        try {
            stopService(Intent(this, BlockVpnService::class.java))
        } catch (_: Exception) {
        }

        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }

        val title = TextView(this).apply {
            text = "STOP HAZARD"
            textSize = 30f
            setTextColor(Color.rgb(183, 28, 28))
            gravity = android.view.Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Blokowanie stron hazardowych"
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 35)
        }

        status = TextView(this).apply {
            text = "Ochrona jest wyłączona"
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        val accessibilityButton = Button(this).apply {
            text = "WŁĄCZ OCHRONĘ"
            setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    Toast.makeText(
                        this@MainActivity,
                        "W Dostępności wybierz STOP HAZARD i włącz usługę. Internet pozostanie normalnie dostępny.",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (_: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Nie udało się otworzyć ustawień Dostępność.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        val info = TextView(this).apply {
            text = "STOP HAZARD korzysta z usługi Dostępność do wykrywania i blokowania stron hazardowych. Nie przejmuje całego ruchu internetowego przez VPN."
            textSize = 15f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 25, 0, 10)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        root.addView(accessibilityButton)
        root.addView(info)
        setContentView(root)
    }
}
