package com.example.stepcounter.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException

/** Сохранение файла в папку, которую пользователь выбрал в системном окне выбора папки. */
object FileSaver {

    /**
     * Создаёт в папке [treeUri] файл [fileName] и записывает в него [bytes].
     * Если файл с таким именем уже есть, система сама добавит к имени номер.
     */
    fun saveToFolder(context: Context, treeUri: Uri, fileName: String, mimeType: String, bytes: ByteArray) {
        val resolver = context.contentResolver
        // Адрес самой папки (из адреса «дерева», который вернул выбор папки)
        val folderUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri, DocumentsContract.getTreeDocumentId(treeUri)
        )
        val fileUri = DocumentsContract.createDocument(resolver, folderUri, mimeType, fileName)
            ?: throw IOException("Could not create the file")
        resolver.openOutputStream(fileUri, "wt")?.use { it.write(bytes) }
            ?: throw IOException("Could not open the file for writing")
    }

    /** Читает текстовый файл по адресу [uri] (размер ограничен, чтобы случайно не открыть огромный файл). */
    fun readText(context: Context, uri: Uri, maxBytes: Int = 20_000_000): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Could not open the file")
        if (bytes.size > maxBytes) throw IOException("File is too large")
        return String(bytes, Charsets.UTF_8)
    }
}
