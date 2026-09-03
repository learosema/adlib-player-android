package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AdlibMediaPlayer(
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
            // Adapt font size based on the available width
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
fun AdlibMediaPlayer(modifier: Modifier = Modifier) {
    Surface(
        color = Color.DarkGray,
        modifier = modifier.fillMaxSize()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),

                color = Color.Black,


            ) {
                Column() {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterHorizontally
                        ),
                    ) {

                        FilledIconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play"
                            )
                        }
                        FilledIconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop"
                            )
                        }
                        FilledIconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Rewind"
                            )
                        }
                        FilledIconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Forward"
                            )
                        }
                    } /* end row */
                } /* end col */
            } /* end surface */

            val songs = List(30) { "Song ${it + 1}" }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                // You can add a single item (like a header)
                item {
                    Text(
                        text = "My Playlist",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )

                }

                // Or a list of items
                items(songs) { song ->
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))
                    Text(
                        text = song,
                        color = Color.White,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        // Greeting("Lea")
        // GreetingWithButton("Lea")
        AdlibMediaPlayer()
    }
}