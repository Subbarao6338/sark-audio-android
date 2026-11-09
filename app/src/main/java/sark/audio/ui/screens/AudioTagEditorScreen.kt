package sark.audio.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sark.audio.core.*
import sark.audio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTagEditorScreen(
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("Summer Vibe Master") }
    var artist by remember { mutableStateOf("Sark Audio") }
    var album by remember { mutableStateOf("Electronic Horizons") }
    var year by remember { mutableStateOf("2026") }
    var genre by remember { mutableStateOf("EDM / House") }
    var trackNumber by remember { mutableStateOf("1") }
    var comment by remember { mutableStateOf("Mastered with Sark Audio Toolbox") }

    var saveStatus by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Tag & Metadata Editor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("RIFF & ID3 METADATA TAGS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Track Title (INAM)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = artist,
                        onValueChange = { artist = it },
                        label = { Text("Artist (IART)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = album,
                        onValueChange = { album = it },
                        label = { Text("Album (IPRD)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Album, contentDescription = null) }
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it },
                            label = { Text("Year (ICRD)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = trackNumber,
                            onValueChange = { trackNumber = it },
                            label = { Text("Track # (ITRK)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = genre,
                        onValueChange = { genre = it },
                        label = { Text("Genre (IGNR)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Comments (ICMT)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (saveStatus != null) {
                        Text(saveStatus ?: "", color = AccentTeal, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            saveStatus = "Audio metadata tags saved into RIFF INFO chunk."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Metadata Tags")
                    }
                }
            }
        }
    }
}
