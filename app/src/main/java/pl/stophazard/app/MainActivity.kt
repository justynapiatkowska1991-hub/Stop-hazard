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

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var button: Button

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
            text = "Ochrona przed stronami hazardowymi"
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

        button = Button(this).apply {
            text = "WŁĄCZ OCHRONĘ"
            setOnClickListener { requestOrStartVpn() }
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        root.addView(button)

        val accessibilityButton = Button(this).apply {
            text = "WŁĄCZ BLOKADĘ PRZEGLĄDARKI"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        root.addView(accessibilityButton)
        setContentView(root)
    }

    private fun requestOrStartVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            startVpn()
        }
    }

    private fun startVpn() {
        val intent = Intent(this, BlockVpnService::class.java)
            .setAction(BlockVpnService.ACTION_START)
        startService(intent)
        status.text = "Ochrona włączona"
        button.text = "OCHRONA WŁĄCZONA"
        button.isEnabled = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startVpn()
        }
    }

    companion object {
        private const val VPN_REQUEST_CODE = 1001
    }
}