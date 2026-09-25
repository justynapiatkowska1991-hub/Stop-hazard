package pl.stophazard.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.VpnService
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
            setOnClickListener { toggleProtection() }
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
            text = if (BuildConfig.ENABLE_NETSTACK_RUNTIME) {
                "To jest testowa wersja silnika NetValve. Po włączeniu ochrony aplikacja uruchomi VPN i przejmie ruch urządzenia do filtrowania. Jeśli zwykły internet przestanie działać, wyłącz ochronę."
            } else {
                "Ochrona systemowa jest jeszcze niedostępna w tej wersji aplikacji."
            }
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
        if (!BuildConfig.ENABLE_NETSTACK_RUNTIME) {
            status.text = "Ochrona systemowa jest jeszcze niedostępna — silnik testowy nie jest włączony."
            protectButton.text = "OCHRONA JESZCZE NIEDOSTĘPNA"
            protectButton.isEnabled = false
            return
        }

        status.text = "Ochrona systemowa jest wyłączona."
        protectButton.text = "WŁĄCZ OCHRONĘ"
        protectButton.isEnabled = true
    }

    private fun toggleProtection() {
        val serviceIntent = Intent(this, BlockVpnService::class.java)

        if (protectButton.text.toString() == "WYŁĄCZ OCHRONĘ") {
            stopService(serviceIntent)
            status.text = "Ochrona systemowa została wyłączona."
            protectButton.text = "WŁĄCZ OCHRONĘ"
            return
        }

        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            startActivityForResult(prepareIntent, VPN_REQUEST_CODE)
        } else {
            startProtectionService()
        }
    }

    private fun startProtectionService() {
        val serviceIntent = Intent(this, BlockVpnService::class.java)
        startForegroundService(serviceIntent)
        status.text = "Ochrona systemowa jest włączona."
        protectButton.text = "WYŁĄCZ OCHRONĘ"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startProtectionService()
        }
    }

    companion object {
        private const val VPN_REQUEST_CODE = 1001
    }
}
