package sark.audio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sark.audio.ui.theme.*

data class ToolboxTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioDashboardScreen(
    onNavigateToTool: (String) -> Unit
) {
    val tools = listOf(
        ToolboxTool("recorder", "Audio Recorder", "Studio grade PCM audio capture with real-time spectrum", Icons.Default.Mic, AccentCoral),
        ToolboxTool("trimmer", "Audio Cutter / Trimmer", "Sample accurate boundary trimming and split cutting", Icons.Default.ContentCut, PrimaryIndigoLight),
        ToolboxTool("merger", "Audio Merger & Joiner", "Multi-clip sequencer with crossfade blending", Icons.Default.MergeType, AccentTeal),
        ToolboxTool("effects", "Gain Booster & Speed", "Volume amplifier, peak normalizer & tempo transformer", Icons.Default.Speed, AccentAmber),
        ToolboxTool("inspector", "Inspector & Converter", "Format details, bit depth & sample rate inspector", Icons.Default.Info, PrimaryIndigo)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SARK Audio Toolbox", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Professional Audio Suite for Android", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "AUDIO WORKBENCH",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tools) { tool ->
                    ToolboxCard(tool = tool, onClick = { onNavigateToTool(tool.id) })
                }
            }
        }
    }
}

@Composable
fun ToolboxCard(
    tool: ToolboxTool,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tool.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = tool.color,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.title,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tool.description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
