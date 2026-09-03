# Exercise Solution: Media Player Layout

This is a reference implementation of the media player UI using Jetpack Compose.

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MediaPlayerScreen() {
    // Sample data
    val playlist = listOf("Awesome Song.mp3", "Cool Beat.wav", "Nature Sounds.flac", "Podcast Ep 1.mp3")
    var sliderPosition by remember { mutableStateOf(0.3f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 1. Progress Slider
        Text("Now Playing: Awesome Song.mp3", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // 2. Transport Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Prev */ }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
            }
            IconButton(
                onClick = { /* Play/Pause */ },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    Icons.Default.PlayCircle, 
                    contentDescription = "Play",
                    modifier = Modifier.fillMaxSize()
                )
            }
            IconButton(onClick = { /* Next */ }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(8.dp))

        // 3. Playlist
        Text("Up Next", style = MaterialTheme.typography.labelLarge)
        LazyColumn(
            modifier = Modifier.weight(1f) // Makes the list fill remaining space
        ) {
            items(playlist) { fileName ->
                ListItem(
                    headlineContent = { Text(fileName) },
                    leadingContent = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
```

### Key Concepts Used:
*   **`remember { mutableStateOf(...) }`**: How Compose keeps track of things that change (like the slider position).
*   **`Modifier.weight(1f)`**: Tells the `LazyColumn` to take up all the remaining space in the `Column`.
*   **`IconButton` & `Icon`**: Standard way to make interactive graphics.
*   **`ListItem`**: A convenient Material3 component for list rows.
