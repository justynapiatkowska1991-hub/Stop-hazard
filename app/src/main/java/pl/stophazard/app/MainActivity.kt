package pl.stophazard.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private lateinit var manager: ProtectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager = ProtectionManager(this)

        setContent {
            StopHazardScreen(
                manager = manager,
                onRequestVpnConsent = { intent ->
                    startActivityForResult(intent, VPN_REQUEST_CODE)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::manager.isInitialized) manager.sync()
    }

    @Deprecated("Deprecated by Android; retained for compatibility with VPN consent flow.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE) {
            manager.onAuthorizationResult(resultCode)
        }
    }

    companion object {
        private const val VPN_REQUEST_CODE = 7001
    }
}

@Composable
private fun StopHazardScreen(
    manager: ProtectionManager,
    onRequestVpnConsent: (Intent) -> Unit
) {
    var state by remember { mutableStateOf(manager.state()) }
    var customDomains by remember { mutableStateOf(manager.customBlockedDomains().toList().sorted()) }
    var domainInput by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        state = manager.state()
        customDomains = manager.customBlockedDomains().toList().sorted()
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text("STOP HAZARD", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "Blokuj dostęp do stron hazardowych i odzyskaj kontrolę.",
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                if (state.enabled) "OCHRONA AKTYWNA" else "OCHRONA WYŁĄCZONA",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (manager.isVpnAuthorized()) {
                                    "VPN jest autoryzowany przez Androida."
                                } else {
                                    "Do uruchomienia ochrony potrzebna jest zgoda Androida."
                                }
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Blokada")
                                Switch(
                                    checked = state.enabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled) {
                                            when (val result = manager.enable(activity = (LocalContext.current as ComponentActivity))) {
                                                is ProtectionManager.EnableResult.AuthorizationRequired ->
                                                    onRequestVpnConsent(result.intent)
                                                ProtectionManager.EnableResult.Started ->
                                                    message = "Ochrona została uruchomiona."
                                                ProtectionManager.EnableResult.Failed ->
                                                    message = "Nie udało się uruchomić ochrony."
                                            }
                                        } else {
                                            manager.disable()
                                            message = "Ochrona została wyłączona."
                                        }
                                        refresh()
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("PLAN", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (state.tier == SubscriptionTier.PREMIUM) {
                                    "Premium — 29,99 zł / miesiąc lub 159 zł / rok"
                                } else {
                                    "Basic — 19,99 zł / miesiąc"
                                }
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(onClick = {
                                message = "Płatności Google Play zostaną podłączone w kolejnym etapie."
                            }) {
                                Text("Zobacz plany")
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("WŁASNE BLOKADY", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text("Dodaj domenę, którą chcesz dodatkowo zablokować.")
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = domainInput,
                                onValueChange = { domainInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("np. example.com") }
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                if (manager.addBlockedDomain(domainInput)) {
                                    domainInput = ""
                                    message = "Domena dodana do blokady."
                                    refresh()
                                } else {
                                    message = "Nieprawidłowa domena."
                                }
                            }) {
                                Text("Dodaj domenę")
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Twoje dodatkowe domeny",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(customDomains) { domain ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(domain, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = {
                            manager.removeBlockedDomain(domain)
                            refresh()
                        }) {
                            Text("Usuń")
                        }
                    }
                    Divider()
                }

                item {
                    if (message != null) {
                        Text(
                            text = message!!,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
