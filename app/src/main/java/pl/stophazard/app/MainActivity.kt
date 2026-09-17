package pl.stophazard.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

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
            text = "Blokowanie stron hazardowych — wersja testowa"
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
            text = "OCHRONA JESZCZE NIEDOSTĘPNA"
            isEnabled = false
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
                        BlockDecision.BLOCK -> "WYNIK TESTU: BLOKADA — $host"
                        BlockDecision.ALLOW -> "WYNIK TESTU: DOZWOLONA — $host"
                    }
                }
            }
        }

        val info = TextView(this).apply {
            text = "Ochrona systemowa jest obecnie wyłączona. Aplikacja nie uruchamia VPN, nie korzysta z Dostępności, nie przejmuje ruchu i nie odcina internetu. Tester domen pokazuje wyłącznie wynik listy — nie oznacza to, że strona zostanie zablokowana w przeglądarce."
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
        root.addView(info)
        setContentView(root)
        updateProtectionStatus()
    }

    private fun updateProtectionStatus() {
        status.text = "Ochrona systemowa jest wyłączona — silnik filtrowania jest jeszcze w budowie"
        protectButton.text = "OCHRONA JESZCZE NIEDOSTĘPNA"
        protectButton.isEnabled = false
    }
}
