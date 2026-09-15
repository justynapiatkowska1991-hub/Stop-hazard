package pl.stophazard.app

import android.app.Activity
import android.content.ComponentName
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
    private lateinit var protectButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) updateProtectionStatus()
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
            text = "Bezpieczne blokowanie stron hazardowych"
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 25)
        }

        status = TextView(this).apply {
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        protectButton = Button(this).apply {
            text = "WŁĄCZ OCHRONĘ"
            setOnClickListener {
                if (isAccessibilityEnabled()) {
                    Toast.makeText(
                        this@MainActivity,
                        "Ochrona przez Dostępność jest już włączona.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    Toast.makeText(
                        this@MainActivity,
                        "W ustawieniach włącz usługę STOP HAZARD.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        val domainInput = EditText(this).apply {
            hint = "Wpisz domenę, np. sts.pl"
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
            }
        }

        val info = TextView(this).apply {
            text = "Bezpieczny tryb: aplikacja nie uruchamia VPN i nie odcina internetu. Ochrona działa przez opcjonalną usługę Dostępność, która rozpoznaje adresy stron hazardowych w obsługiwanych przeglądarkach."
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
        updateProtectionStatus()
    }

    private fun updateProtectionStatus() {
        val enabled = isAccessibilityEnabled()
        status.text = if (enabled) {
            "Ochrona Dostępności jest włączona"
        } else {
            "Ochrona jest wyłączona — włącz Dostępność"
        }
        protectButton.text = if (enabled) "OCHRONA JEST WŁĄCZONA" else "WŁĄCZ OCHRONĘ"
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, GamblingAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(':').any { value ->
            ComponentName.unflattenFromString(value) == expected
        }
    }
}
