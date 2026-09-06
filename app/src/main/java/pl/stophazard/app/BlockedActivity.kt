package pl.stophazard.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class BlockedActivity : Activity() {

    companion object {
        const val EXTRA_HOST = "blocked_host"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val host = intent.getStringExtra(EXTRA_HOST) ?: "strona hazardowa"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = "STOP HAZARD"
            textSize = 30f
            setTextColor(Color.rgb(183, 28, 28))
            gravity = Gravity.CENTER
        }

        val message = TextView(this).apply {
            text = "Ta strona została zablokowana.\n\n$host"
            textSize = 19f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 32)
        }

        val close = Button(this).apply {
            text = "ZAMKNIJ"
            setOnClickListener { finish() }
        }

        root.addView(title)
        root.addView(message)
        root.addView(close)
        setContentView(root)
    }
}
