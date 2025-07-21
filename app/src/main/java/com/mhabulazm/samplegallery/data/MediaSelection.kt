package com.mhabulazm.samplegallery.data

import android.provider.MediaStore
import android.util.Log
import com.mhabulazm.samplegallery.domain.entity.MediaFilter

fun buildMediaSelection(filter: MediaFilter, albumId: String?): Pair<String, Array<String>> {
    val selectionParts = mutableListOf<String>()
    val selectionArgs = mutableListOf<String>()

    when {
        albumId == "CAMERA" -> {
            selectionParts.add("${MediaStore.MediaColumns.BUCKET_DISPLAY_NAME} = ?")
            selectionArgs.add("Camera")
        }

        albumId != null && !albumId.startsWith("ALL_") -> {
            selectionParts.add("${MediaStore.MediaColumns.BUCKET_ID} = ?")
            selectionArgs.add(albumId)
        }

    }
    // Size filtering
    if (filter.minSizeBytes > 0) {
        selectionParts.add("${MediaStore.MediaColumns.SIZE} >= ?")
        selectionArgs.add(filter.minSizeBytes.toString())
    }
    if (filter.maxSizeBytes < Long.MAX_VALUE) {
        selectionParts.add("${MediaStore.MediaColumns.SIZE} <= ?")
        selectionArgs.add(filter.maxSizeBytes.toString())
    }

    // Date filtering
    if (filter.afterDate > 0) {
        selectionParts.add("${MediaStore.MediaColumns.DATE_TAKEN} >= ?")
        selectionArgs.add(filter.afterDate.toString())
    }
    if (filter.beforeDate < Long.MAX_VALUE) {
        selectionParts.add("${MediaStore.MediaColumns.DATE_TAKEN} <= ?")
        selectionArgs.add(filter.beforeDate.toString())
    }

    // MIME type filtering
    if (filter.mimeTypes.isNotEmpty()) {
        val mimeConditions = filter.mimeTypes.map {
            when (it) {
                "image/*" -> "${MediaStore.MediaColumns.MIME_TYPE} LIKE 'image/%'"
                "video/*" -> "${MediaStore.MediaColumns.MIME_TYPE} LIKE 'video/%'"
                else -> "${MediaStore.MediaColumns.MIME_TYPE} = '$it'"
            }
        }
        Log.e("MediaSelection", "buildMediaSelection: ${selectionParts.size}")
        selectionParts.add("(${mimeConditions.joinToString(" OR ")})")
    }

    return Pair(
        selectionParts.joinToString(" AND "),
        selectionArgs.toTypedArray()
    )
}