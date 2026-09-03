# Getting Started with Android UI (Jetpack Compose)

Welcome to Android development! In modern Android, we use **Jetpack Compose** to build UIs. Instead of XML, you write Kotlin functions called **Composables**.

## 1. Layout Containers (The Skeleton)
These define how elements are positioned:
*   **Column**: Positions elements vertically.
*   **Row**: Positions elements horizontally.
*   **Box**: Stacks elements on top of each other.
*   **Scaffold**: Provides a standard screen structure (TopBar, FAB, etc.).

## 2. Basic UI Elements
The building blocks of your app:
*   **Text**: Displays strings (e.g., `Text("Hello")`).
*   **Button**: Clickable element (e.g., `Button(onClick = { ... }) { Text("Click") }`).
*   **Image**: Displays icons or photos.
*   **TextField**: Input field for user text.

## 3. Lists (Efficient Scrolling)
For many items, use "Lazy" versions that only render what's visible. This is critical for performance.

### How LazyColumn works:
You use the `items()` function inside the `LazyColumn` block to map your data to Composables.

```kotlin
val songs = listOf("Song A", "Song B", "Song C", "Song D")

LazyColumn {
    // You can add a single item (like a header)
    item {
        Text("My Playlist", style = MaterialTheme.typography.titleLarge)
    }
    
    // Or a list of items
    items(songs) { song ->
        Text(
            text = song,
            modifier = Modifier.padding(8.dp).fillMaxWidth()
        )
    }
}
```

## 4. Modifiers (The Secret Sauce)
Almost every Composable takes a `Modifier`. Use them to change:
*   **Size**: `Modifier.size(100.dp)` or `Modifier.fillMaxSize()`.
*   **Padding**: `Modifier.padding(16.dp)`.
*   **Background**: `Modifier.background(Color.Blue)`.
*   **Input**: `Modifier.clickable { ... }`.

## 5. Simple Example
Here is how you combine these elements:

```kotlin
@Composable
fun WelcomeScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome to Android!", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { /* Action here */ }) {
            Text("Get Started")
        }
    }
}
```

---

## 🎯 First Exercise: Media Player Layout
Now it's your turn! Try to build a basic media player UI.

**Goal:** Create a screen that contains:
1.  **Transport Controls**: A `Row` with "Previous", "Play/Pause", and "Next" icons.
2.  **Progress**: A `Slider` to show song progress.
3.  **Playlist**: A `LazyColumn` below the controls showing a list of filenames.

*Tip: Use `Icons.Default.PlayArrow`, `Icons.Default.Pause`, etc. from the Material library.*

Check the `docs/exercise_solution.md` for a reference implementation!

Happy coding!
