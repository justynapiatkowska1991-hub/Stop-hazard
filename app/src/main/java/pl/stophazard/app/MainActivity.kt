package pl.stophazard.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            text = "Ochrona jest chwilowo niedostępna"
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        val protectButton = Button(this).apply {
            text = "SPRAWDŹ OCHRONĘ"
            setOnClickListener {
                status.text = "Filtr jest jeszcze w przygotowaniu"
                Toast.makeText(
                    this@MainActivity,
                    "Ochrona zostanie włączona po zakończeniu bezpiecznych testów.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val accessibilityButton = Button(this).apply {
            text = "USTAWIENIA DOSTĘPNOŚCI"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(
                    this@MainActivity,
                    "Usługa Dostępność jest opcjonalna i nie zastępuje filtra.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val info = TextView(this).apply {
            text = "Aplikacja jest w bezpiecznej wersji testowej. Internet pozostaje dostępny, a filtr hazardowy zostanie uruchomiony dopiero po pozytywnych testach."
            textSize = 15f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 25, 0, 10)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        root.addView(protectButton)
        root.addView(accessibilityButton)
        root.addView(info)
        setContentView(root)
    }
}
