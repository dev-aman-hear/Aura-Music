package com.aman.auramusic.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aman.auramusic.party.LocalPartyServer
import com.aman.auramusic.party.PartyConnectionMode
import com.aman.auramusic.party.PartyModeRepository
import com.aman.auramusic.party.PartyRole
import kotlinx.coroutines.launch

@Composable
fun PartyModeScreen(
    repository: PartyModeRepository,
    onBack: () -> Unit,
    onHostCommand: (String) -> Unit = {},
    onApprovedSongRequest: (String, String, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var localHostAddress by remember { mutableStateOf("") }
    var localPairingCode by remember { mutableStateOf("") }
    var requestTitle by remember { mutableStateOf("") }
    var requestArtist by remember { mutableStateOf("") }

    LaunchedEffect(state.session) {
        if (state.session == null) repository.startLocalDiscovery()
        else repository.stopLocalDiscovery()
    }

    DisposableEffect(Unit) {
        onDispose { repository.stopLocalDiscovery() }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Party Mode", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Listen together, in sync",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = "Party Mode",
                        modifier = Modifier.padding(12.dp).size(25.dp)
                    )
                }
            }

            // Hero card keeps the key limitation visible without dominating the screen.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Shared playback", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Aura Music sends track metadata and playback controls—not audio. Each listener plays the track from their own supported source.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoPill(text = "Internet")
                        InfoPill(text = "Local Wi-Fi")
                        InfoPill(text = "No audio relay")
                    }
                }
            }

            if (state.session == null) {
                SectionHeading(title = "Start a party", subtitle = "Create a room, then invite listeners.")
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(32) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Display name (optional)") },
                    placeholder = { Text("Aura listener") },
                    singleLine = true
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.padding(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Internet party", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("For friends on different networks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Button(
                            onClick = { scope.launch { repository.createParty(displayName) } },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.connecting && repository.isConfigured()
                        ) { Text("Create Internet Party") }
                        if (!repository.isConfigured()) {
                            Text(
                                "A secure HTTPS/WSS backend is required for Internet parties.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.padding(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Local Wi-Fi party", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("For devices on the same Wi-Fi or hotspot", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedButton(
                            onClick = { scope.launch { repository.createLocalParty(displayName) } },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.connecting
                        ) { Text("Create Local Wi-Fi Party") }
                        Text(
                            "Local mode uses an unencrypted LAN connection. Join only on a trusted network.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SectionHeading(title = "Join an Internet party", subtitle = "Enter the code shared by the host.")
                OutlinedTextField(
                    value = pairingCode,
                    onValueChange = { pairingCode = it.uppercase().filter(Char::isLetterOrDigit).take(12) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Party code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                Button(
                    onClick = { scope.launch { repository.joinParty(pairingCode, displayName) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.connecting && repository.isConfigured() && pairingCode.length >= 4
                ) { Text("Join Internet Party") }

                SectionHeading(title = "Nearby parties", subtitle = "Available hosts on your local network.")
                if (state.discoveredParties.isEmpty()) {
                    Text(
                        "Searching for local parties… You can also enter the host IP manually below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.discoveredParties.forEach { party ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(party.displayName.ifBlank { "Local Party" }, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${party.hostAddress}:${party.port} · Code ${party.pairingCode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        localHostAddress = party.hostAddress
                                        localPairingCode = party.pairingCode
                                        scope.launch { repository.joinLocalParty(party.hostAddress, party.pairingCode, displayName) }
                                    },
                                    enabled = !state.connecting
                                ) { Text("Join") }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = localHostAddress,
                    onValueChange = { localHostAddress = it.filter { ch -> ch.isLetterOrDigit() || ch == '.' || ch == ':' }.take(64) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Host IP address") },
                    placeholder = { Text("192.168.1.10") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = localPairingCode,
                    onValueChange = { localPairingCode = it.uppercase().filter(Char::isLetterOrDigit).take(12) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Local party code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                OutlinedButton(
                    onClick = { scope.launch { repository.joinLocalParty(localHostAddress, localPairingCode, displayName) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.connecting && localHostAddress.isNotBlank() && localPairingCode.length >= 4
                ) { Text("Join Local Wi-Fi Party") }
            } else {
                val session = state.session!!
                val isHost = session.role == PartyRole.HOST
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Icon(
                                    if (session.connectionMode == PartyConnectionMode.LOCAL_WIFI) Icons.Default.Wifi else Icons.Default.Groups,
                                    contentDescription = null,
                                    modifier = Modifier.padding(12.dp).size(26.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(if (isHost) "You're hosting" else "You're listening", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    if (session.connectionMode == PartyConnectionMode.LOCAL_WIFI) "LOCAL WI-FI" else "INTERNET",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            ConnectionIndicator(connected = state.connected, connecting = state.connecting)
                        }
                        Text(
                            state.status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricTile(
                                modifier = Modifier.weight(1f),
                                label = "Listeners",
                                value = state.members.toString(),
                                icon = { Icon(Icons.Default.Groups, contentDescription = null) }
                            )
                            MetricTile(
                                modifier = Modifier.weight(1f),
                                label = "Transport",
                                value = if (session.connectionMode == PartyConnectionMode.LOCAL_WIFI) "Wi-Fi" else "Internet",
                                icon = { Icon(Icons.Default.Wifi, contentDescription = null) }
                            )
                        }

                        if (isHost && session.pairingCode.isNotBlank()) {
                            HorizontalDivider()
                            Text("INVITE CODE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    session.pairingCode,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Aura Music Party code", session.pairingCode))
                                    Toast.makeText(context, "Party code copied", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy invite code")
                                }
                            }
                        }

                        if (session.connectionMode == PartyConnectionMode.LOCAL_WIFI) {
                            val hostAddress = session.hostAddress
                            if (!hostAddress.isNullOrBlank()) {
                                Text(
                                    if (isHost) "Share this address: $hostAddress:${LocalPartyServer.DEFAULT_PORT}" else "Host address: $hostAddress",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        HorizontalDivider()
                        SectionHeading(
                            title = "Now playing",
                            subtitle = if (state.playback == null) "Waiting for the host to start a supported online song." else "Playback state shared by the host."
                        )
                        val playback = state.playback
                        if (playback == null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Text("Waiting for playback metadata…", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.padding(16.dp).size(28.dp))
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        playback.title.ifBlank { "Unknown track" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        playback.artist.ifBlank { "Unknown artist" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${playback.source} · ${if (playback.isPlaying) "Playing" else "Paused"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (isHost) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { onHostCommand("previous") }) {
                                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous track")
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        IconButton(onClick = { onHostCommand(if (playback.isPlaying) "pause" else "play") }) {
                                            Icon(
                                                if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (playback.isPlaying) "Pause" else "Play",
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onHostCommand("next") }) {
                                        Icon(Icons.Default.SkipNext, contentDescription = "Next track")
                                    }
                                }
                            }
                        }
                        OutlinedButton(onClick = { repository.leaveParty() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Leave Party")
                        }
                    }
                }

                if (session.role == PartyRole.GUEST) {
                    SectionHeading(title = "Request a song", subtitle = "Ask the host to play a track from a supported source.")
                    OutlinedTextField(
                        value = requestTitle,
                        onValueChange = { requestTitle = it.take(128) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Song title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = requestArtist,
                        onValueChange = { requestArtist = it.take(128) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Artist") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                repository.requestSong(requestTitle.trim(), requestArtist.trim(), "YOUTUBE")
                                requestTitle = ""
                                requestArtist = ""
                            },
                            enabled = state.connected && requestTitle.isNotBlank() && requestArtist.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) { Text("YouTube Music") }
                        OutlinedButton(
                            onClick = {
                                repository.requestSong(requestTitle.trim(), requestArtist.trim(), "JIOSAAVN")
                                requestTitle = ""
                                requestArtist = ""
                            },
                            enabled = state.connected && requestTitle.isNotBlank() && requestArtist.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) { Text("JioSaavn") }
                    }
                    state.decisionMessage?.let { message ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(message, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (isHost && state.requests.isNotEmpty()) {
                    SectionHeading(title = "Song requests", subtitle = "${state.requests.size} waiting for your decision.")
                    state.requests.forEach { request ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(request.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${request.artist} · ${request.source} · ${request.requester}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            repository.respondToRequest(request.requestId, true)
                                            onApprovedSongRequest(request.title, request.artist, request.source)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Approve") }
                                    OutlinedButton(
                                        onClick = { repository.respondToRequest(request.requestId, false) },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Decline") }
                                }
                            }
                        }
                    }
                }
            }

            if (state.connecting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("  Connecting to party…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            state.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Text(
                "Party Mode syncs metadata and controls only. Playback availability and timing depend on each device's source access and network.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoPill(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ConnectionIndicator(connected: Boolean, connecting: Boolean) {
    val label = when {
        connected -> "Connected"
        connecting -> "Connecting"
        else -> "Offline"
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = if (connected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (connecting) CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
            else if (connected) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            icon()
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
