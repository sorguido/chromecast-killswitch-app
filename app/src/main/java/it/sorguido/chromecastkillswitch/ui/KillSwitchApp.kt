// SPDX-License-Identifier: GPL-3.0-or-later
// Original app code; see LICENSE and LICENSE-ADDITIONAL-PERMISSION.
package it.sorguido.chromecastkillswitch.ui

import it.sorguido.chromecastkillswitch.transport.Endpoint
import it.sorguido.chromecastkillswitch.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.text.DateFormat
import java.util.Date
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Valori presentati solo quando il controller li ha rilevati; nessun dato simulato nella UI. */
data class UiState(
    val targetName: String? = null,
    val connection: UiText = UiText(R.string.connection_disconnected),
    val configuration: UiText = UiText(R.string.configuration_unverified),
    val detail: UiText = UiText(R.string.detail_connect),
    val lastRead: Long? = null,
    val progress: UiText? = null,
    val pending: Boolean = false,
    val busy: Boolean = false,
    val canOperate: Boolean = false,
    val paired: Boolean = false,
    val discoveryRunning: Boolean = false,
    val pairingEndpoints: List<UiEndpoint> = emptyList(),
    val connectEndpoints: List<UiEndpoint> = emptyList(),
    val error: UiText? = null,
)

data class UiEndpoint(val id: String, val label: String, val host: String, val port: Int)

data class UiActions(
    val onRefresh: () -> Unit,
    val onDiscover: () -> Unit,
    val onPair: (host: String, port: Int, code: String) -> Unit,
    val onConnect: (host: String, port: Int) -> Unit,
    val onOperate: (enabled: Boolean) -> Unit,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B60), onPrimary = Color.White,
    primaryContainer = Color(0xFFCCEDE5), onPrimaryContainer = Color(0xFF00382F),
    secondary = Color(0xFF435F58), onSecondary = Color.White,
    background = Color(0xFFF5F7F3), onBackground = Color(0xFF182520),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF182520),
    surfaceVariant = Color(0xFFE6EDE7), onSurfaceVariant = Color(0xFF43534A),
    outline = Color(0xFF68796E), error = Color(0xFF9E2535),
    errorContainer = Color(0xFFFFDADD), onErrorContainer = Color(0xFF690014),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF89D4C3), onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF184D43), onPrimaryContainer = Color(0xFFCCEDE5),
    secondary = Color(0xFFB3CDC2), onSecondary = Color(0xFF20392F),
    background = Color(0xFF101A16), onBackground = Color(0xFFE5EEE7),
    surface = Color(0xFF19261F), onSurface = Color(0xFFE5EEE7),
    surfaceVariant = Color(0xFF2B3C32), onSurfaceVariant = Color(0xFFBDCEC2),
    outline = Color(0xFF8B9F91), error = Color(0xFFFFB2BB),
    errorContainer = Color(0xFF6D1C2A), onErrorContainer = Color(0xFFFFDADD),
)

@Composable
fun KillSwitchApp(state: UiState, actions: UiActions) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors) {
        var section by remember { mutableStateOf<String?>(null) }
        var requestedOperation by remember { mutableStateOf<Boolean?>(null) }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 560.dp).fillMaxWidth()
                        .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.eyebrow), style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.app_heading), style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
                        Text(stringResource(R.string.tagline),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(state.targetName ?: stringResource(R.string.target_default), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text(state.connection.resolve(), color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Text(stringResource(R.string.configuration_heading), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.configuration.resolve(), style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                            Text(state.detail.resolve(), style = MaterialTheme.typography.bodyMedium)
                            state.lastRead?.let {
                                Text(stringResource(R.string.last_read, DateFormat.getTimeInstance(DateFormat.SHORT, LocalConfiguration.current.locales[0]).format(Date(it))), style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (state.busy || state.progress != null || state.pending) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                                Text(state.progress?.resolve() ?: stringResource(R.string.operation_unverified), fontWeight = FontWeight.Medium,
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                                if (state.pending) Text(
                                    stringResource(R.string.pending_warning),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    state.error?.let { error ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.error_heading), style = MaterialTheme.typography.titleMedium)
                                Text(error.resolve(), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                                Text(stringResource(R.string.wireless_off_warning),
                                    style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { section = "help" }, modifier = Modifier.heightIn(min = 48.dp)) {
                                    Text(stringResource(R.string.open_recovery))
                                }
                            }
                        }
                    }
                    if (!state.paired) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.start_tv), style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.semantics { heading() })
                            Text(stringResource(R.string.pair_intro))
                            PrimaryAction(stringResource(R.string.pair_chromecast), !state.busy) {
                                section = "pair"; actions.onDiscover()
                            }
                        }
                    } else if (state.targetName == null) {
                        Text(stringResource(R.string.paired_choose_port))
                        PrimaryAction(stringResource(R.string.connect_chromecast), !state.busy) {
                            section = "connect"; actions.onDiscover()
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryAction(stringResource(R.string.enable_reboot), state.canOperate && !state.busy) {
                            requestedOperation = true
                        }
                        OutlinedButton(
                            onClick = { requestedOperation = false }, enabled = state.canOperate && !state.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        ) { Text(stringResource(R.string.disable_reboot), style = MaterialTheme.typography.titleMedium) }
                        Text(stringResource(R.string.reboot_tunnel_warning),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Column {
                        TextButton(onClick = actions.onRefresh, enabled = state.paired && !state.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.refresh)) }
                        TextButton(onClick = { section = if (state.paired) "connect" else "pair"; actions.onDiscover() },
                            enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.connection_pairing))
                        }
                        TextButton(onClick = { section = "help" }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.help_recovery))
                        }
                    }
                }
            }
        }
        requestedOperation?.let { enabled ->
            SheetDialog(if (enabled) stringResource(R.string.confirm_enable_title) else stringResource(R.string.confirm_disable_title), { requestedOperation = null }) {
                Text(stringResource(R.string.confirm_network))
                Text(stringResource(R.string.confirm_reboot))
                Text(if (enabled) stringResource(R.string.confirm_enable_body)
                    else stringResource(R.string.confirm_disable_body))
                OutlinedButton(onClick = { requestedOperation = null }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.cancel))
                }
                PrimaryAction(stringResource(R.string.confirm), !state.busy && state.canOperate) {
                    requestedOperation = null
                    actions.onOperate(enabled)
                }
            }
        }
        when (section) {
            "pair" -> ConnectionDialog(state, actions, pairing = true, close = { section = null },
                changeMode = { section = "connect" })
            "connect" -> ConnectionDialog(state, actions, pairing = false, close = { section = null },
                changeMode = { section = "pair" })
            "help" -> HelpDialog { section = null }
        }
    }
}

@Composable
private fun PrimaryAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SheetDialog(title: String, close: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Dialog has its own composition root; retain the same resource configuration as the screen.
        CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides configuration) {
            Surface(modifier = Modifier.padding(20.dp).widthIn(max = 520.dp).fillMaxWidth().fillMaxHeight(0.9f),
                shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { heading() })
                    content()
                }
            }
        }
    }
}

@Composable
private fun ConnectionDialog(state: UiState, actions: UiActions, pairing: Boolean, close: () -> Unit, changeMode: () -> Unit) {
    var host by remember(pairing) { mutableStateOf("") }
    var port by remember(pairing) { mutableStateOf("") }
    // Il codice non entra in SavedState, preferenze o diagnostica.
    var code by remember(pairing) { mutableStateOf("") }
    var attempted by remember(pairing) { mutableStateOf(false) }
    val normalizedHost = host.trim().removePrefix("[").removeSuffix("]")
    val portNumber = port.toIntOrNull()
    val validHost = runCatching { Endpoint(normalizedHost, 1) }.isSuccess
    val validPort = portNumber != null && portNumber in 1..65535
    val validCode = !pairing || code.matches(Regex("[0-9]{6}"))
    SheetDialog(if (pairing) stringResource(R.string.pair_phone) else stringResource(R.string.connect_chromecast), close) {
        TextButton(onClick = close, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.back_home)) }
        if (pairing) {
            Text(stringResource(R.string.pair_step_one))
            Text(stringResource(R.string.pair_step_two))
            Text(stringResource(R.string.pair_step_three))
            Text(stringResource(R.string.pair_authorization_note),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(stringResource(R.string.connect_instructions))
            Text(stringResource(R.string.connect_authorization_note),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.error?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        state.progress?.let { Text(it.resolve(), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(stringResource(R.string.local_network), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        val endpoints = if (pairing) state.pairingEndpoints else state.connectEndpoints
        if (endpoints.isEmpty()) Text(if (state.discoveryRunning) stringResource(R.string.discovery_running)
            else stringResource(R.string.discovery_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        endpoints.forEach { endpoint ->
            OutlinedButton(onClick = { host = endpoint.host; port = endpoint.port.toString(); attempted = false },
                enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                contentPadding = PaddingValues(16.dp)) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(endpoint.label.ifBlank { stringResource(R.string.endpoint_default) }, style = MaterialTheme.typography.labelLarge)
                    Text("${endpoint.host} · ${endpoint.port}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        OutlinedButton(onClick = actions.onDiscover, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.search_again)) }
        HorizontalDivider()
        Text(if (pairing) stringResource(R.string.pair_address) else stringResource(R.string.connect_address), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = host, onValueChange = { host = it }, enabled = !state.busy,
            label = { Text(stringResource(R.string.ip_address)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), isError = attempted && !validHost,
            supportingText = if (attempted && !validHost) ({ Text(stringResource(R.string.invalid_address)) }) else null)
        OutlinedTextField(value = port, onValueChange = { port = it.filter(Char::isDigit).take(5) }, enabled = !state.busy,
            label = { Text(if (pairing) stringResource(R.string.pair_port) else stringResource(R.string.connect_port)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = attempted && !validPort,
            supportingText = if (attempted && !validPort) ({ Text(stringResource(R.string.invalid_port)) }) else null)
        if (pairing) OutlinedTextField(value = code, onValueChange = { code = it.filter { c -> c in '0'..'9' }.take(6) },
            enabled = !state.busy, label = { Text(stringResource(R.string.pair_code)) }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(), isError = attempted && !validCode,
            supportingText = { Text(if (attempted && !validCode) stringResource(R.string.invalid_code) else stringResource(R.string.code_privacy)) })
        PrimaryAction(if (pairing) stringResource(R.string.pair_this_phone) else stringResource(R.string.connect_read), !state.busy && (pairing || state.paired)) {
            attempted = true
            if (validHost && validPort && validCode) {
                if (pairing) {
                    val submittedCode = code
                    code = ""
                    actions.onPair(normalizedHost, portNumber!!, submittedCode)
                } else {
                    actions.onConnect(normalizedHost, portNumber!!)
                    close()
                }
            }
        }
        if (pairing && state.paired) {
            Text(stringResource(R.string.pair_saved))
            PrimaryAction(stringResource(R.string.switch_connect), !state.busy, changeMode)
        } else if (!pairing) {
            TextButton(onClick = changeMode, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.pair_again))
            }
        }
    }
}

@Composable
private fun HelpDialog(close: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var linkError by remember { mutableStateOf(false) }
    SheetDialog(stringResource(R.string.help_recovery), close) {
        TextButton(onClick = close, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.back_home)) }
        HelpSection(stringResource(R.string.help_target_title), stringResource(R.string.help_target_body))
        HelpSection(stringResource(R.string.help_discovery_title), stringResource(R.string.help_discovery_body))
        HelpSection(stringResource(R.string.help_pair_title), stringResource(R.string.help_pair_body))
        HelpSection(stringResource(R.string.enable_reboot), stringResource(R.string.help_enable_body))
        HelpSection(stringResource(R.string.disable_reboot), stringResource(R.string.help_disable_body))
        HelpSection(stringResource(R.string.help_reboot_title), stringResource(R.string.help_reboot_body))
        HelpSection(stringResource(R.string.help_partial_title), stringResource(R.string.help_partial_body))
        HelpSection(stringResource(R.string.help_revoked_title), stringResource(R.string.help_revoked_body))
        HelpSection(stringResource(R.string.help_usb_title), stringResource(R.string.help_usb_body))
        TextButton(onClick = {
            try { uriHandler.openUri("https://github.com/sorguido/chromecast-killswitch-app/blob/main/docs/COMMAND_CONTRACT.md") }
            catch (_: Exception) { linkError = true }
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.usb_repository)) }
        Text(stringResource(R.string.offline_note),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (linkError) Text(stringResource(R.string.link_error), color = MaterialTheme.colorScheme.error)
        PrimaryAction(stringResource(R.string.understood), onClick = close)
    }
}

@Composable
private fun HelpSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() })
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}
