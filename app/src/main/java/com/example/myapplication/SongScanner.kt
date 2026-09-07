package com.example.myapplication

import android.content.Context
import android.os.Environment
import java.io.File
import java.util.Locale

class SongScanner(private val context: Context) {

    fun scanDocuments(): List<Song> {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!documentsDir.exists() || !documentsDir.isDirectory) {
            return emptyList()
        }

        val songFiles = documentsDir.listFiles { file ->
            val name = file.name.lowercase(Locale.getDefault())
            file.isFile && (name.endsWith(".mid") || name.endsWith(".xmi") || name.endsWith(".cmf"))
        } ?: emptyArray()

        return songFiles.map { file ->
            Song(title = file.nameWithoutExtension, path = file.absolutePath)
        }.sortedBy { it.title }
    }
}
