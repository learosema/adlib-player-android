package com.example.myapplication

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.util.Locale

class SongScanner(private val context: Context) {

    fun scanDocuments(): List<Song> {
        val rootDir = Environment.getExternalStorageDirectory()
        Log.d("SongScanner", "Scanning root: ${rootDir.absolutePath}")
        
        if (!rootDir.exists()) {
            return emptyList()
        }

        val results = mutableListOf<Song>()
        val extensions = listOf(".mid", ".cmf", ".mus", ".hmi", ".hmp", ".imf", ".xmi")
        
        fun scan(file: File) {
            if (file.isDirectory) {
                // Skip system folders that are likely to cause issues or be slow
                if (file.name == "Android" || file.name.startsWith(".")) return
                
                val files = file.listFiles() ?: return
                for (f in files) scan(f)
            } else {
                val name = file.name.lowercase(Locale.getDefault())
                if (extensions.any { name.endsWith(it) }) {
                    Log.d("SongScanner", "Found: ${file.absolutePath}")
                    results.add(Song(title = file.nameWithoutExtension, path = file.absolutePath))
                }
            }
        }

        try {
            scan(rootDir)
        } catch (e: Exception) {
            Log.e("SongScanner", "Error during scan", e)
        }
            
        return results.sortedBy { it.title }
    }
}
