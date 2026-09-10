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
import android.util.Log
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to get MediaController", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        super.onStop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
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
                    var uiSelectedSongIndex by remember { mutableStateOf(0) }
                    var cursorIndex by remember { mutableStateOf(0) }
                    var currentTrackIndex by remember { mutableStateOf(0) }
                    var tracksCount by remember { mutableStateOf(1) }

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

                    val mediaItems = remember(songs) {
                        songs.map { s ->
                            MediaItem.Builder()
                                .setMediaId(s.path)
                                .setUri(Uri.parse(s.path))
                                .setMediaMetadata(MediaMetadata.Builder().setTitle(s.title).build())
                                .build()
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

                    LaunchedEffect(mediaController) {
                        while (true) {
                            mediaController?.let { controller ->
                                isPlaying = controller.isPlaying
                                val duration = controller.duration
                                if (duration > 0) {
                                    currentProgress = controller.currentPosition.toFloat() / duration.toFloat()
                                }
                                if (isPlaying) {
                                    uiSelectedSongIndex = controller.currentMediaItemIndex
                                }
                            }
                            delay(500)
                        }
                    }

                    AdlibMediaPlayer(
                        songs = songs,
                        progress = currentProgress,
                        isPlaying = isPlaying,
                        selectedSongIndex = uiSelectedSongIndex,
                        cursorIndex = cursorIndex,
                        onCursorMove = { index -> cursorIndex = index },
                        onSongSelect = { index ->
                            uiSelectedSongIndex = index
                            cursorIndex = index
                            val controller = mediaController ?: return@AdlibMediaPlayer
                            controller.setMediaItems(mediaItems, index, 0L)
                            controller.prepare()
                            controller.play()
                        },
                        onRescan = {
                            songs = SongScanner(context).scanDocuments()
                        },
                        onPlayPause = { song ->
                            val controller = mediaController ?: return@AdlibMediaPlayer
                            if (isPlaying) {
                                controller.pause()
                            } else {
                                if (controller.currentMediaItem?.mediaId == song.path) {
                                    controller.play()
                                } else {
                                    val index = songs.indexOf(song).coerceAtLeast(0)
                                    controller.setMediaItems(mediaItems, index, 0L)
                                    controller.prepare()
                                    controller.play()
                                }
                            }
                        },
                        onSeek = { progress ->
                            mediaController?.let { controller ->
                                val duration = controller.duration
                                if (duration > 0) {
                                    controller.seekTo((progress * duration).toLong())
                                }
                            }
                            currentProgress = progress
                        },
                        onForward = {
                            mediaController?.let { controller ->
                                val duration = controller.duration
                                val current = controller.currentPosition
                                if (duration > 0) {
                                    val newPos = (current + 10000L).coerceAtMost(duration)
                                    controller.seekTo(newPos)
                                }
                            }
                        },
                        onRewind = {
                            mediaController?.let { controller ->
                                val current = controller.currentPosition
                                val newPos = (current - 10000L).coerceAtLeast(0L)
                                controller.seekTo(newPos)
                            }
                        },
                        onNext = {
                            mediaController?.seekToNext()
                        },
                        onPrevious = {
                            mediaController?.seekToPrevious()
                        },
                        onStop = {
                            mediaController?.stop()
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
    selectedSongIndex: Int,
    cursorIndex: Int,
    onCursorMove: (Int) -> Unit,
    onSongSelect: (Int) -> Unit,
    onRescan: () -> Unit,
    onPlayPause: (Song) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onForward: () -> Unit,
    onRewind: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong = if (songs.isNotEmpty() && selectedSongIndex < songs.size) {
        songs[selectedSongIndex]
    } else {
        null
    }
    val triggerPlayPause = { currentSong?.let { onPlayPause(it) }; Unit }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        color = Color.Black,
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> {
                        if (songs.isNotEmpty()) onCursorMove((cursorIndex + 1).coerceAtMost(songs.size - 1))
                        true
                    }
                    Key.DirectionUp -> {
                        if (songs.isNotEmpty()) onCursorMove((cursorIndex - 1).coerceAtLeast(0))
                        true
                    }
                    Key.Enter, Key.NumPadEnter -> {
                        if (songs.isNotEmpty()) onSongSelect(cursorIndex.coerceIn(0, songs.size - 1))
                        true
                    }
                    Key.Spacebar, Key.MediaPlayPause -> {
                        triggerPlayPause()
                        true
                    }
                    Key.MediaPlay -> {
                        if (!isPlaying) triggerPlayPause()
                        true
                    }
                    Key.MediaPause -> {
                        if (isPlaying) triggerPlayPause()
                        true
                    }
                    Key.DirectionRight -> {
                        onForward()
                        true
                    }
                    Key.DirectionLeft -> {
                        onRewind()
                        true
                    }
                    Key.MediaNext -> {
                        onNext()
                        true
                    }
                    Key.MediaPrevious -> {
                        onPrevious()
                        true
                    }
                    Key.MediaStop -> {
                        onStop()
                        true
                    }
                    else -> false
                }
            }
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
                cursorIndex = cursorIndex,
                onCursorMove = onCursorMove,
                onSongSelect = onSongSelect,
                modifier = Modifier.weight(1f)
            )
            PlaybackProgress(
                songTitle = currentSong?.title ?: "No Songs Found",
                progress = progress,
                onProgressChange = onSeek
            )
            PlayerControls(
                isPlaying = isPlaying,
                onPlayPause = { triggerPlayPause() },
                onNext = onNext,
                onPrevious = onPrevious,
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
    cursorIndex: Int,
    onCursorMove: (Int) -> Unit,
    onSongSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(cursorIndex) {
        // Only scroll when the cursor actually moves off-screen (keyboard navigation past the
        // visible area) - a tap on a row that's already visible shouldn't cause any scrolling.
        val alreadyVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == cursorIndex }
        if (cursorIndex in songs.indices && !alreadyVisible) {
            listState.animateScrollToItem(cursorIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth()
    ) {
        items(songs.size) { index ->
            PlaylistItem(
                song = songs[index].title,
                isPlaying = index == selectedSongIndex,
                isCursor = index == cursorIndex,
                onClick = { onCursorMove(index) },
                onDoubleClick = { onSongSelect(index) }
            )
        }
    }
}

@Composable
fun PlaylistItem(
    song: String,
    isPlaying: Boolean,
    isCursor: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isCursor) Color.White.copy(alpha = 0.15f) else Color.Transparent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .pointerInput(onClick, onDoubleClick) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleClick() }
                )
            }
    ) {
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))
        Text(
            text = song,
            color = if (isPlaying) Color.White else Color.Green,
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
            selectedSongIndex = 0,
            cursorIndex = 0,
            onCursorMove = {},
            onSongSelect = {},
            onRescan = {},
            onPlayPause = {},
            onNext = {},
            onPrevious = {},
            onSeek = {},
            onForward = {},
            onRewind = {},
            onStop = {}
        )
    }
}
