package pl.stophazard.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var protectButton: Button

    companion object {
        private const val VPN_REQUEST_CODE = 4107
        private const val PREFS = "stop_hazard_protection"
        private const val KEY_ACTIVE = "active"
    }

    private val runtimeAvailable: Boolean
        get() = BuildConfig.USE_NETSTACK && BuildConfig.ENABLE_NETSTACK_RUNTIME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) updateProtectionStatus()
    }

    @Deprecated("Uses the platform VPN consent activity result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) startProtectionService()
            else {
                status.text = "Nie udzielono zgody na VPN. Ochrona pozostaje wyłączona."
                updateProtectionButton(false)
            }
        }
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
            setOnClickListener {
                val active = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ACTIVE, false)
                if (active) stopProtectionService() else requestVpnPermission()
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
                        BlockDecision.BLOCK -> "WYNIK TESTU: BLOKADA — $host"
                        BlockDecision.ALLOW -> "WYNIK TESTU: DOZWOLONA — $host"
                    }
                }
            }
        }

        val info = TextView(this).apply {
            text = if (runtimeAvailable) {
                "Tryb testowy silnika VPN. Po włączeniu sprawdź zwykłe strony oraz domeny hazardowe. Jeśli internet przestanie działać, wyłącz ochronę przyciskiem poniżej."
            } else {
                "Silnik VPN jest dołączany w osobnym buildzie testowym. W tej wersji ochrona pozostaje wyłączona; tester domen pokazuje wyłącznie wynik listy i nie blokuje stron w przeglądarce."
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

    private fun requestVpnPermission() {
        if (!runtimeAvailable) {
            status.text = "Ta wersja nie zawiera aktywnego silnika VPN."
            return
        }
        val permissionIntent = VpnService.prepare(this)
        if (permissionIntent != null) {
            startActivityForResult(permissionIntent, VPN_REQUEST_CODE)
        } else {
            startProtectionService()
        }
    }

    private fun startProtectionService() {
        val intent = Intent(this, BlockVpnService::class.java).apply {
            action = BlockVpnService.ACTION_START
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
            status.text = "Uruchamianie ochrony VPN…"
        } catch (_: Exception) {
            status.text = "Nie udało się uruchomić VPN. Ochrona pozostaje wyłączona."
            updateProtectionButton(false)
        }
    }

    private fun stopProtectionService() {
        val intent = Intent(this, BlockVpnService::class.java).apply {
            action = BlockVpnService.ACTION_STOP
        }
        runCatching { startService(intent) }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, false).apply()
        status.text = "Ochrona wyłączona."
        updateProtectionButton(false)
    }

    private fun updateProtectionStatus() {
        val active = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ACTIVE, false)
        if (active && runtimeAvailable) {
            status.text = "OCHRONA VPN JEST WŁĄCZONA — WERSJA TESTOWA"
        } else if (!runtimeAvailable) {
            status.text = "Ochrona systemowa jest wyłączona — oczekiwanie na testowy build silnika"
        } else {
            status.text = "Ochrona jest wyłączona"
        }
        updateProtectionButton(active)
    }

    private fun updateProtectionButton(active: Boolean) {
        protectButton.text = when {
            !runtimeAvailable -> "OCHRONA JESZCZE NIEDOSTĘPNA"
            active -> "WYŁĄCZ OCHRONĘ"
            else -> "WŁĄCZ OCHRONĘ"
        }
        protectButton.isEnabled = runtimeAvailable
    }
}
