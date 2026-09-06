package pl.stophazard.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.content.Intent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

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
            text = "Włącz blokadę stron hazardowych poniżej"
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        val accessibilityButton = Button(this).apply {
            text = "WŁĄCZ BLOKADĘ"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val info = TextView(this).apply {
            text = "STOP HAZARD nie uruchamia już VPN. Dzięki temu zwykły Internet, poczta i inne strony nie są odcinane."
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

    override fun onResume() {
        super.onResume()
        status.text = "Blokada działa przez Usługi ułatwień dostępu"
    }
}
