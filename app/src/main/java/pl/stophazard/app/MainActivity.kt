package pl.stophazard.app

import android.app.Activity
import android.graphics.Color
import android.net.VpnService
import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.provider.Settings
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
            text = "Ochrona jest wyłączona"
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        val protectionButton = Button(this).apply {
            text = "WŁĄCZ OCHRONĘ"
            setOnClickListener {
                val intent = VpnService.prepare(this@MainActivity)
                if (intent != null) {
                    startActivityForResult(intent, VPN_REQUEST_CODE)
                } else {
                    startProtection()
                }
            }
        }

        val info = TextView(this).apply {
            text = "STOP HAZARD używa lokalnego VPN do filtrowania domen hazardowych. Zwykły Internet pozostaje dostępny."
            textSize = 15f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 25, 0, 10)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        root.addView(protectionButton)

        val accessibilityButton = Button(this).apply {
            text = "WŁĄCZ DODATKOWĄ BLOKADĘ"
            setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    Toast.makeText(
                        this@MainActivity,
                        "W ustawieniach Dostępność wybierz STOP HAZARD i włącz usługę.",
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

        root.addView(accessibilityButton)
        root.addView(info)
        setContentView(root)
    }

    private fun startProtection() {
        val intent = Intent(this, BlockVpnService::class.java).apply {
            action = BlockVpnService.ACTION_START
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        status.text = "Ochrona VPN jest aktywna"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startProtection()
            } else {
                status.text = "Ochrona VPN nie została włączona"
                Toast.makeText(this, "Zezwól na połączenie VPN, aby włączyć ochronę.", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val VPN_REQUEST_CODE = 1001
    }
}
