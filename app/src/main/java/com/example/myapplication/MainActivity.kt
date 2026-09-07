package com.example.myapplication

import android.Manifest
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var playbackService: PlaybackService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlaybackService.LocalBinder
            playbackService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            playbackService = null
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, PlaybackService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val context = LocalContext.current
                    var songs by remember { mutableStateOf(emptyList<Song>()) }
                    var currentProgress by remember { mutableStateOf(0f) }
                    var isPlaying by remember { mutableStateOf(false) }

                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }

                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            songs = SongScanner(context).scanDocuments()
                        }
                    }

                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (!Environment.isExternalStorageManager()) {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                    intent.data = Uri.parse("package:${context.packageName}")
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    context.startActivity(intent)
                                }
                            } else {
                                songs = SongScanner(context).scanDocuments()
                            }
                        } else {
                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                songs = SongScanner(context).scanDocuments()
                            } else {
                                launcher.launch(permission)
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        while (true) {
                            val servicePlaying = playbackService?.audioPlayer?.isPlaying ?: false
                            if (isPlaying != servicePlaying) {
                                isPlaying = servicePlaying
                            }
                            if (servicePlaying) {
                                currentProgress = playbackService?.audioPlayer?.getProgress() ?: 0f
                            }
                            delay(500)
                        }
                    }

                    AdlibMediaPlayer(
                        songs = songs,
                        progress = currentProgress,
                        isPlaying = isPlaying,
                        onRescan = {
                            songs = SongScanner(context).scanDocuments()
                        },
                        onPlayPause = { song ->
                            if (isPlaying) {
                                playbackService?.stopPlayback()
                            } else {
                                val intent = Intent(context, PlaybackService::class.java)
                                intent.putExtra("PATH", song.path)
                                ContextCompat.startForegroundService(context, intent)
                            }
                        },
                        onSongPlay = { song ->
                            val intent = Intent(context, PlaybackService::class.java)
                            intent.putExtra("PATH", song.path)
                            ContextCompat.startForegroundService(context, intent)
                        },
                        onSeek = { progress ->
                            playbackService?.audioPlayer?.seek(progress)
                            currentProgress = progress
                        },
                        onForward = {
                            val newProgress = (currentProgress + 0.1f).coerceAtMost(1f)
                            playbackService?.audioPlayer?.seek(newProgress)
                            currentProgress = newProgress
                        },
                        onRewind = {
                            val newProgress = (currentProgress - 0.1f).coerceAtLeast(0f)
                            playbackService?.audioPlayer?.seek(newProgress)
                            currentProgress = newProgress
                        },
                        onStop = {
                            playbackService?.stopPlayback()
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.Magenta,
        modifier = modifier.fillMaxSize()
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            val adaptiveFontSize = (maxWidth.value / 6).sp
            Text(
                text = "Moin $name!",
                fontSize = adaptiveFontSize,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = adaptiveFontSize * 1.2f
            )
        }
    }
}

@Composable
fun GreetingWithButton(name: String) {
    Surface(
        color = Color.DarkGray,
        modifier = Modifier.fillMaxSize()
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            val adaptiveFontSize = (maxWidth.value / 6).sp
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text="Moin $name", fontSize = adaptiveFontSize)
                Button(onClick = {}) {
                    Text("Okay")
                }
            }
        }
    }
}

@Composable
fun AdlibMediaPlayer(
    songs: List<Song>,
    progress: Float,
    isPlaying: Boolean,
    onRescan: () -> Unit,
    onPlayPause: (Song) -> Unit,
    onSongPlay: (Song) -> Unit,
    onSeek: (Float) -> Unit,
    onForward: () -> Unit,
    onRewind: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSongIndex by remember { mutableStateOf(0) }

    Surface(
        color = Color.Black,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Library (${songs.size})",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onRescan) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rescan Library",
                        tint = Color.White
                    )
                }
            }
            PlaylistView(
                songs = songs,
                selectedSongIndex = selectedSongIndex,
                onSongSelect = { selectedSongIndex = it },
                onSongPlay = { index ->
                    selectedSongIndex = index
                    onSongPlay(songs[index])
                },
                modifier = Modifier.weight(1f)
            )
            val currentSong = if (songs.isNotEmpty() && selectedSongIndex < songs.size) {
                songs[selectedSongIndex]
            } else {
                null
            }
            PlaybackProgress(
                songTitle = currentSong?.title ?: "No Songs Found",
                progress = progress,
                onProgressChange = onSeek
            )
            PlayerControls(
                isPlaying = isPlaying,
                onPlayPause = {
                    currentSong?.let { onPlayPause(it) }
                },
                onNext = {
                    if (songs.isNotEmpty()) {
                        selectedSongIndex = (selectedSongIndex + 1) % songs.size
                        onSongPlay(songs[selectedSongIndex])
                    }
                },
                onPrevious = {
                    if (songs.isNotEmpty()) {
                        selectedSongIndex = if (selectedSongIndex > 0) selectedSongIndex - 1 else songs.size - 1
                        onSongPlay(songs[selectedSongIndex])
                    }
                },
                onForward = onForward,
                onRewind = onRewind,
                onStop = onStop
            )
        }
    }
}

@Composable
fun PlaybackProgress(
    songTitle: String,
    progress: Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "Now Playing: $songTitle",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = progress,
                onValueChange = onProgressChange,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.Gray
                )
            )
        }
    }
}

@Composable
fun PlayerControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onForward: () -> Unit,
    onRewind: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(
                8.dp,
                Alignment.CenterHorizontally
            )
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White
                )
            }
            IconButton(onClick = onRewind) {
                Icon(
                    imageVector = Icons.Default.FastRewind,
                    contentDescription = "Rewind",
                    tint = Color.White
                )
            }
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
            IconButton(onClick = onForward) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = "Forward",
                    tint = Color.White
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White
                )
            }
            IconButton(onClick = onStop) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun PlaylistView(
    songs: List<Song>,
    selectedSongIndex: Int,
    onSongSelect: (Int) -> Unit,
    onSongPlay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {
        items(songs.size) { index ->
            PlaylistItem(
                song = songs[index].title,
                isSelected = index == selectedSongIndex,
                onClick = { onSongSelect(index) },
                onDoubleClick = { onSongPlay(index) }
            )
        }
    }
}

@Composable
fun PlaylistItem(
    song: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleClick() }
                )
            }
    ) {
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))
        Text(
            text = song,
            color = if (isSelected) Color.White else Color.Green,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        AdlibMediaPlayer(
            songs = listOf(Song("Sample Song", "/path/to/song.mid")),
            progress = 0.5f,
            isPlaying = false,
            onRescan = {},
            onPlayPause = {},
            onSongPlay = {},
            onSeek = {},
            onForward = {},
            onRewind = {},
            onStop = {}
        )
    }
}
