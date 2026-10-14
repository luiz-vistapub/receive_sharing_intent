package com.kasem.receive_sharing_intent

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.util.*
import android.webkit.MimeTypeMap
import android.util.Log


object FileDirectory {

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    fun getAbsolutePath(context: Context, uri: Uri): String? {
        // Security fix (Memoryboard fork): only content:// URIs are accepted
        // and the data is always copied into a dedicated cache folder under a
        // name generated here. Upstream builds the target path from the
        // sharing app's _display_name (path traversal, "Dirty Stream") and
        // returns file:// and _data paths as-is, so a malicious app could make
        // this app overwrite or read its own private files.
        if (!"content".equals(uri.scheme, ignoreCase = true)) return null
        return copyToPrivateCache(context, uri)
    }

    private fun copyToPrivateCache(context: Context, uri: Uri): String? {
        val dir = File(context.cacheDir, "receive_sharing_intent")
        if (!dir.exists() && !dir.mkdirs()) return null
        val mimeType = context.contentResolver.getType(uri)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            ?.takeIf { it.matches(Regex("[A-Za-z0-9]{1,8}")) }
        val prefix = when {
            mimeType?.startsWith("image") == true -> "IMG"
            mimeType?.startsWith("video") == true -> "VID"
            else -> "FILE"
        }
        val name = "${prefix}_${UUID.randomUUID()}" + (extension?.let { ".$it" } ?: "")
        val target = File(dir, name)
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { stream ->
                FileOutputStream(target).use { out -> stream.copyTo(out) }
            }
            target.path
        } catch (e: Exception) {
            Log.w("FileDirectory", "Could not copy shared content", e)
            target.delete()
            null
        }
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }
}