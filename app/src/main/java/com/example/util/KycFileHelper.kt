package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

data class SavedKycFile(
    val internalPath: String,
    val displayName: String
)

object KycFileHelper {
    private const val KYC_DIR = "kyc"

    fun copyIntoPrivateStorage(context: Context, sourceUri: Uri): SavedKycFile? {
        return try {
            val resolver = context.contentResolver
            var originalName = "kyc_document"
            resolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    originalName = cursor.getString(nameIndex) ?: originalName
                }
            }
            val mimeType = resolver.getType(sourceUri) ?: ""
            val ext = when {
                originalName.contains('.') -> originalName.substringAfterLast('.', "dat")
                mimeType.contains("pdf", ignoreCase = true) -> "pdf"
                mimeType.contains("png", ignoreCase = true) -> "png"
                mimeType.contains("webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }.lowercase()

            val dir = File(context.filesDir, KYC_DIR).apply { if (!exists()) mkdirs() }
            val destFile = File(dir, "kyc_${UUID.randomUUID()}.$ext")
            resolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            SavedKycFile(
                internalPath = destFile.absolutePath,
                displayName = originalName
            )
        } catch (_: Exception) {
            null
        }
    }

    fun deleteFileIfExists(internalPath: String?) {
        if (internalPath.isNullOrBlank()) return
        try {
            val file = File(internalPath)
            if (file.exists() && file.absolutePath.contains("/$KYC_DIR/")) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }

    fun isImageFile(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val lower = path.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".bmp")
    }

    fun openExternally(context: Context, internalPath: String): Boolean {
        return try {
            val file = File(internalPath)
            if (!file.exists()) return false
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mime = when {
                internalPath.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                isImageFile(internalPath) -> "image/*"
                else -> "*/*"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open KYC Document"))
            true
        } catch (_: Exception) {
            false
        }
    }
}
