package pl.stophazard.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
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
            setPadding(0, 20, 0, 25)
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
                status.text = "Filtr ruchu jest jeszcze w przygotowaniu"
                Toast.makeText(
                    this@MainActivity,
                    "Ochrona zostanie włączona po zakończeniu bezpiecznych testów.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val domainInput = EditText(this).apply {
            hint = "Wpisz domenę do sprawdzenia, np. sts.pl"
            maxLines = 1
            setPadding(12, 20, 12, 20)
        }

        val checkDomainButton = Button(this).apply {
            text = "SPRAWDŹ DOMENĘ"
            setOnClickListener {
                val host = domainInput.text.toString()
                if (host.isBlank()) {
                    status.text = "Wpisz domenę do sprawdzenia"
                } else {
                    status.text = when (DomainFilter.decision(host)) {
                        BlockDecision.BLOCK -> "WYNIK: BLOKADA — $host"
                        BlockDecision.ALLOW -> "WYNIK: DOZWOLONA — $host"
                    }
                }
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
            text = "Wersja testowa: możesz sprawdzić, czy domena znajduje się na liście blokad. Internet pozostaje dostępny. Aktywne filtrowanie zostanie uruchomione dopiero po pozytywnych testach."
            textSize = 15f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 25, 0, 10)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        root.addView(protectButton)
        root.addView(domainInput)
        root.addView(checkDomainButton)
        root.addView(accessibilityButton)
        root.addView(info)
        setContentView(root)
    }
}
