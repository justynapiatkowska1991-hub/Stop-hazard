package pl.stophazard.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private lateinit var controller: ProtectionController
    private lateinit var repository: ProtectionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = ProtectionController(this)
        repository = ProtectionRepository(this)

        setContent {
            StopHazardApp(
                initiallyEnabled = repository.state().enabled,
                vpnAuthorized = controller.isAuthorized(),
                onToggle = { enabled ->
                    repository.setProtectionEnabled(enabled)
                    if (enabled) {
                        val prepareIntent = controller.prepareVpn()
                        if (prepareIntent != null) {
                            startActivityForResult(prepareIntent, VPN_REQUEST_CODE)
                        } else {
                            controller.start()
                        }
                    } else {
                        controller.stop()
                    }
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized && repository.state().enabled && controller.isAuthorized()) {
            controller.start()
        }
    }

    @Deprecated("Deprecated by Android; retained for compatibility with VPN consent flow.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            controller.start()
        }
    }

    companion object {
        private const val VPN_REQUEST_CODE = 7001
    }
}

@Composable
private fun StopHazardApp(
    initiallyEnabled: Boolean,
    vpnAuthorized: Boolean,
    onToggle: (Boolean) -> Unit
) {
    var blockingEnabled by remember { mutableStateOf(initiallyEnabled) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("STOP HAZARD", style = MaterialTheme.typography.headlineLarge)
                Text(
                    if (vpnAuthorized) "Ochrona VPN jest gotowa."
                    else "Android wymaga zgody na uruchomienie ochrony.",
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
                )
                Button(onClick = {
                    val next = !blockingEnabled
                    blockingEnabled = next
                    onToggle(next)
                }) {
                    Text(if (blockingEnabled) "Blokada aktywna" else "Włącz blokadę")
                }
            }
        }
    }
}
