package pl.stophazard.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }

        val title = TextView(this).apply {
            text = "STOP HAZARD"
            textSize = 30f
            setTextColor(Color.rgb(183, 28, 28))
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Ochrona przed stronami hazardowymi"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 40)
        }

        status = TextView(this).apply {
            text = "Ochrona jest wyłączona"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        val button = Button(this).apply {
            text = "WŁĄCZ OCHRONĘ"
            setOnClickListener {
                status.text = "Ochrona włączona — moduł blokowania zostanie dodany w kolejnym etapie."
                this.text = "OCHRONA WŁĄCZONA"
                isEnabled = false
            }
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        root.addView(button)
        setContentView(root)
    }
}
