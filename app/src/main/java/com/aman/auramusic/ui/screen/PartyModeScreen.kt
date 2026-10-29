package com.aman.auramusic.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.party.LocalPartyServer
import com.aman.auramusic.party.PartyConnectionMode
import com.aman.auramusic.party.PartyModeRepository
import com.aman.auramusic.party.PartyQrHelper
import com.aman.auramusic.party.PartyRole
import com.aman.auramusic.party.ScannedParty
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

@Composable
fun PartyModeScreen(
    repository: PartyModeRepository,
    onBack: () -> Unit,
    onHostCommand: (String) -> Unit = {},
    onApprovedSongRequest: (String, String, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var displayName by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var localHostAddress by remember { mutableStateOf("") }
    var localPairingCode by remember { mutableStateOf("") }
    var requestTitle by remember { mutableStateOf("") }
    var requestArtist by remember { mutableStateOf("") }
    var selectedConnectionMode by remember { mutableStateOf(PartyConnectionMode.INTERNET) }
    var showJoinOptions by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    val isInternetConfigured = repository.isConfigured()
    val colors = MaterialTheme.colorScheme

    fun handleScannedCode(raw: String) {
        when (val parsed = PartyQrHelper.parsePartyQr(raw)) {
            is ScannedParty.Internet -> {
                pairingCode = parsed.code
                Toast.makeText(context, "Joining Internet Party: ${parsed.code}", Toast.LENGTH_SHORT).show()
                scope.launch { repository.joinParty(parsed.code, displayName) }
            }
            is ScannedParty.Local -> {
                localHostAddress = parsed.host
                localPairingCode = parsed.code
                Toast.makeText(context, "Joining Local Party at ${parsed.host}…", Toast.LENGTH_SHORT).show()
                scope.launch { repository.joinLocalParty(parsed.host, parsed.code, displayName) }
            }
            null -> Toast.makeText(context, "Invalid or unrecognized party QR code", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraScannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.takeIf(String::isNotBlank)?.let(::handleScannedCode)
    }
    val galleryPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val decoded = PartyQrHelper.decodeQrFromUri(context, uri)
            if (!decoded.isNullOrBlank()) {
                handleScannedCode(decoded)
            } else {
                Toast.makeText(context, "No QR code found in selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(state.session) {
        if (state.session == null) repository.startLocalDiscovery()
        else {
            repository.stopLocalDiscovery()
            showJoinOptions = false
        }
    }
    DisposableEffect(Unit) {
        onDispose { repository.stopLocalDiscovery() }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Party Mode",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(20.dp),
                    tint = colors.onSurfaceVariant
                )
            }

            HorizontalDivider(color = colors.outlineVariant)

            if (state.session == null) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "Start a party",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Choose how friends will connect.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ConnectionModeOption(
                            modifier = Modifier.weight(1f),
                            label = "Internet",
                            icon = { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            selected = selectedConnectionMode == PartyConnectionMode.INTERNET,
                            onClick = { selectedConnectionMode = PartyConnectionMode.INTERNET }
                        )
                        ConnectionModeOption(
                            modifier = Modifier.weight(1f),
                            label = "Local Wi-Fi",
                            icon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            selected = selectedConnectionMode == PartyConnectionMode.LOCAL_WIFI,
                            onClick = { selectedConnectionMode = PartyConnectionMode.LOCAL_WIFI }
                        )
                    }

                    Text(
                        text = when (selectedConnectionMode) {
                            PartyConnectionMode.INTERNET -> if (isInternetConfigured) {
                                "Secure connection · Invite friends on any network."
                            } else {
                                "Internet parties need a secure backend and are unavailable right now."
                            }
                            PartyConnectionMode.LOCAL_WIFI ->
                                "Everyone must be on the same Wi-Fi or hotspot. Local connections are unencrypted; use a trusted network."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    when (selectedConnectionMode) {
                                        PartyConnectionMode.INTERNET -> repository.createParty(displayName)
                                        PartyConnectionMode.LOCAL_WIFI -> repository.createLocalParty(displayName)
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp),
                            enabled = !state.connecting &&
                                (selectedConnectionMode != PartyConnectionMode.INTERNET || isInternetConfigured),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.onSurface,
                                contentColor = colors.surface
                            )
                        ) {
                            Text("Start a Party", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { showJoinOptions = !showJoinOptions },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)
                        ) {
                            Text(if (showJoinOptions) "Close Join" else "Join a Party", maxLines = 1)
                        }
                    }
                }

                if (state.session == null && showJoinOptions) {
                    HorizontalDivider(color = colors.outlineVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                "Join a party",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Enter an invite code, scan a QR code, or find a host nearby.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = pairingCode,
                            onValueChange = { pairingCode = it.uppercase().filter(Char::isLetterOrDigit).take(12) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Internet party code") },
                            singleLine = true,
                            enabled = isInternetConfigured && !state.connecting,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                        )
                        Button(
                            onClick = { scope.launch { repository.joinParty(pairingCode, displayName) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            enabled = !state.connecting && isInternetConfigured && pairingCode.length >= 4,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.onSurface,
                                contentColor = colors.surface
                            )
                        ) {
                            Text("Join Internet Party")
                        }
                        if (!isInternetConfigured) {
                            Text(
                                "Internet parties need a secure backend. Local Wi-Fi joining is still available below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val options = ScanOptions().apply {
                                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                        setPrompt("Scan party QR code")
                                        setBeepEnabled(true)
                                        setOrientationLocked(false)
                                    }
                                    cameraScannerLauncher.launch(options)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text("Scan QR")
                            }
                            OutlinedButton(
                                onClick = { galleryPickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text("Choose image")
                            }
                        }

                        HorizontalDivider(color = colors.outlineVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Nearby Local Wi-Fi", fontWeight = FontWeight.Medium)
                            Text(
                                "Connect to the host's Wi-Fi or hotspot first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }

                        if (state.discoveredParties.isEmpty()) {
                            Text(
                                "Searching for nearby parties…",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        } else {
                            state.discoveredParties.forEach { party ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, tint = colors.onSurfaceVariant)
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            party.displayName.ifBlank { "Local Party" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "${party.hostAddress}:${party.port} · ${party.pairingCode}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            localHostAddress = party.hostAddress
                                            localPairingCode = party.pairingCode
                                            scope.launch {
                                                repository.joinLocalParty(party.hostAddress, party.pairingCode, displayName)
                                            }
                                        },
                                        enabled = !state.connecting,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)
                                    ) {
                                        Text("Join")
                                    }
                                }
                                HorizontalDivider(color = colors.outlineVariant)
                            }
                        }

                        OutlinedTextField(
                            value = localHostAddress,
                            onValueChange = {
                                localHostAddress = it.filter { ch ->
                                    ch.isLetterOrDigit() || ch == '.' || ch == ':'
                                }.take(64)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Host IP address") },
                            placeholder = { Text("192.168.1.10") },
                            singleLine = true,
                            enabled = !state.connecting
                        )
                        OutlinedTextField(
                            value = localPairingCode,
                            onValueChange = { localPairingCode = it.uppercase().filter(Char::isLetterOrDigit).take(12) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Local party code") },
                            singleLine = true,
                            enabled = !state.connecting,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                        )
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    repository.joinLocalParty(localHostAddress, localPairingCode, displayName)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            enabled = !state.connecting &&
                                localHostAddress.isNotBlank() &&
                                localPairingCode.length >= 4,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)
                        ) {
                            Text("Join Local Wi-Fi Party")
                        }
                    }
                }
            } else {
                val session = state.session!!
                val isHost = session.role == PartyRole.HOST
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            if (session.connectionMode == PartyConnectionMode.LOCAL_WIFI) {
                                Icons.Default.Wifi
                            } else {
                                Icons.Default.Groups
                            },
                            contentDescription = null,
                            tint = colors.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                if (isHost) "You're hosting" else "You're listening",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (session.connectionMode == PartyConnectionMode.LOCAL_WIFI) "Local Wi-Fi" else "Internet",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        ConnectionIndicator(connected = state.connected, connecting = state.connecting)
                    }

                    Text(state.status, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${state.members} ${if (state.members == 1) "listener" else "listeners"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant
                        )
                        Text("·", color = colors.onSurfaceVariant)
                        Text(
                            if (session.connectionMode == PartyConnectionMode.LOCAL_WIFI) "Local Wi-Fi" else "Internet",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant
                        )
                    }

                    if (isHost && session.pairingCode.isNotBlank()) {
                        HorizontalDivider(color = colors.outlineVariant)
                        Text(
                            "INVITE CODE",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                session.pairingCode,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 3.sp
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Aura Music Party code", session.pairingCode))
                                    Toast.makeText(context, "Party code copied", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy invite code")
                            }
                            IconButton(
                                onClick = { showQrDialog = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = "Show Party QR code")
                            }
                        }
                    }

                    if (session.connectionMode == PartyConnectionMode.LOCAL_WIFI) {
                        session.hostAddress?.takeIf(String::isNotBlank)?.let { hostAddress ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (isHost) "Host: $hostAddress:${LocalPartyServer.DEFAULT_PORT}" else "Host: $hostAddress",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val addressToCopy = if (isHost) "$hostAddress:${LocalPartyServer.DEFAULT_PORT}" else hostAddress
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Party Host IP", addressToCopy))
                                        Toast.makeText(context, "Host address copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy host address")
                                }
                            }
                        }
                    }

                    if (isHost) {
                        OutlinedButton(
                            onClick = { showQrDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Show Party QR Code")
                        }
                    }

                    HorizontalDivider(color = colors.outlineVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Now playing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (state.playback == null) "Waiting for the host to start a supported online song."
                                else "Playback shared by the host.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        val playback = state.playback
                        if (playback == null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("Waiting for playback metadata…", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.onSurfaceVariant)
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        playback.title.ifBlank { "Unknown track" },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        playback.artist.ifBlank { "Unknown artist" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                    Text(
                                        "${playback.source} · ${if (playback.isPlaying) "Playing" else "Paused"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }
                            if (isHost) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { onHostCommand("previous") }, modifier = Modifier.size(48.dp)) {
                                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous track")
                                    }
                                    IconButton(
                                        onClick = { onHostCommand(if (playback.isPlaying) "pause" else "play") },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .border(1.dp, colors.outlineVariant, CircleShape)
                                    ) {
                                        Icon(
                                            if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (playback.isPlaying) "Pause" else "Play"
                                        )
                                    }
                                    IconButton(onClick = { onHostCommand("next") }, modifier = Modifier.size(48.dp)) {
                                        Icon(Icons.Default.SkipNext, contentDescription = "Next track")
                                    }
                                }
                            }
                        }
                    }

                    if (session.role == PartyRole.GUEST) {
                        HorizontalDivider(color = colors.outlineVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Request a song", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Ask the host to play a supported track.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            OutlinedTextField(
                                value = requestTitle,
                                onValueChange = { requestTitle = it.take(128) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Song title") },
                                singleLine = true,
                                enabled = state.connected
                            )
                            OutlinedTextField(
                                value = requestArtist,
                                onValueChange = { requestArtist = it.take(128) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Artist") },
                                singleLine = true,
                                enabled = state.connected
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        repository.requestSong(requestTitle.trim(), requestArtist.trim(), "YOUTUBE")
                                        requestTitle = ""
                                        requestArtist = ""
                                    },
                                    enabled = state.connected && requestTitle.isNotBlank() && requestArtist.isNotBlank(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.onSurface,
                                        contentColor = colors.surface
                                    )
                                ) {
                                    Text("YouTube Music")
                                }
                                OutlinedButton(
                                    onClick = {
                                        repository.requestSong(requestTitle.trim(), requestArtist.trim(), "JIOSAAVN")
                                        requestTitle = ""
                                        requestArtist = ""
                                    },
                                    enabled = state.connected && requestTitle.isNotBlank() && requestArtist.isNotBlank(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)
                                ) {
                                    Text("JioSaavn")
                                }
                            }
                            state.decisionMessage?.let { message ->
                                Text(message, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                        }
                    }

                    if (isHost && state.requests.isNotEmpty()) {
                        HorizontalDivider(color = colors.outlineVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Song requests · ${state.requests.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            state.requests.forEach { request ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(request.title, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${request.artist} · ${request.source} · ${request.requester}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = {
                                                repository.respondToRequest(request.requestId, true)
                                                onApprovedSongRequest(request.title, request.artist, request.source)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colors.onSurface,
                                                contentColor = colors.surface
                                            )
                                        ) {
                                            Text("Approve")
                                        }
                                        OutlinedButton(
                                            onClick = { repository.respondToRequest(request.requestId, false) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)
                                        ) {
                                            Text("Decline")
                                        }
                                    }
                                }
                                if (request != state.requests.last()) {
                                    HorizontalDivider(color = colors.outlineVariant)
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { repository.leaveParty() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)
                    ) {
                        Text("Leave Party")
                    }
                }
            }

            if (state.connecting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text("Connecting to party…", style = MaterialTheme.typography.bodySmall)
                }
            }
            state.error?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.errorContainer
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(12.dp),
                        color = colors.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Text(
                "Party Mode shares music metadata and controls, not audio. Playback depends on each device's source access and network.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 8.dp)
            )
        }
    }

    if (showQrDialog && state.session != null) {
        val currentSession = state.session!!
        val qrPayload = remember(currentSession) {
            if (currentSession.connectionMode == PartyConnectionMode.LOCAL_WIFI) {
                val hostIp = currentSession.hostAddress ?: "127.0.0.1"
                PartyQrHelper.createLocalPayload(hostIp, LocalPartyServer.DEFAULT_PORT, currentSession.pairingCode)
            } else {
                PartyQrHelper.createInternetPayload(currentSession.pairingCode)
            }
        }
        val qrBitmap = remember(qrPayload) { PartyQrHelper.generateQrBitmap(qrPayload, 600) }

        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = {
                Text(
                    if (currentSession.connectionMode == PartyConnectionMode.LOCAL_WIFI) {
                        "Local Party QR Code"
                    } else {
                        "Internet Party QR Code"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Have other listeners scan this QR code with Aura Music to join immediately.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (qrBitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Party QR code for ${currentSession.pairingCode}",
                                modifier = Modifier
                                    .size(240.dp)
                                    .padding(12.dp)
                            )
                        }
                    } else {
                        Text(
                            "QR code could not be generated.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error
                        )
                    }
                    Text(
                        text = "Code: ${currentSession.pairingCode}" +
                            if (currentSession.connectionMode == PartyConnectionMode.LOCAL_WIFI &&
                                !currentSession.hostAddress.isNullOrBlank()
                            ) {
                                "\nHost: ${currentSession.hostAddress}:${LocalPartyServer.DEFAULT_PORT}"
                            } else {
                                ""
                            },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ConnectionModeOption(
    modifier: Modifier,
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.onSurface else colors.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) colors.onSurface else colors.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(Modifier.size(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) colors.surface else colors.onSurface
            )
        }
    }
}

@Composable
private fun ConnectionIndicator(connected: Boolean, connecting: Boolean) {
    val colors = MaterialTheme.colorScheme
    val label = when {
        connected -> "Connected"
        connecting -> "Connecting"
        else -> "Offline"
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = colors.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (connecting) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
            } else if (connected) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
            }
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
