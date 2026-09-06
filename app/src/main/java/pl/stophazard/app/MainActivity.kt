package pl.stophazard.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.VpnService
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
            text = "Ochrona jest wyłączona"
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        val protectButton = Button(this).apply {
            text = "WŁĄCZ OCHRONĘ"
            setOnClickListener {
                startProtection()
            }
        }

        val accessibilityButton = Button(this).apply {
            text = "DODATKOWO WŁĄCZ DOSTĘPNOŚĆ"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(
                    this@MainActivity,
                    "Wybierz STOP HAZARD i włącz usługę.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val info = TextView(this).apply {
            text = "STOP HAZARD używa VPN do filtrowania zapytań DNS oraz usługi Dostępność jako dodatkowej ochrony."
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

    private fun startProtection() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST)
            return
        }
        startVpnService()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST) {
            if (resultCode == RESULT_OK) {
                startVpnService()
            } else {
                status.text = "VPN nie został zaakceptowany"
                Toast.makeText(
                    this,
                    "Aby blokowanie działało, zaakceptuj połączenie VPN.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startVpnService() {
        val serviceIntent = Intent(this, BlockVpnService::class.java)
        try {
            startForegroundService(serviceIntent)
            status.text = "Ochrona jest aktywna"
            Toast.makeText(this, "STOP HAZARD — ochrona aktywna", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            status.text = "Nie udało się uruchomić ochrony"
            Toast.makeText(
                this,
                "Błąd uruchamiania ochrony: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        private const val VPN_REQUEST = 1001
    }
}
