package com.mhabulazm.samplegallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mhabulazm.samplegallery.domain.entity.MediaFilter
import com.mhabulazm.samplegallery.domain.entity.MediaItem
import com.mhabulazm.samplegallery.domain.entity.MediaType
import javax.inject.Inject

class MediaPagingSource @Inject constructor(
    private val contentResolver: ContentResolver,
    private val mediaType: MediaType,
    private val filter: MediaFilter,
    private val albumId: String?,
) : PagingSource<Int, MediaItem>() {
    companion object {
        private const val TAG = "MediaPagingSource"
    }

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        return try {
            val (selection, selectionArgs) = buildMediaSelection(filter, albumId)
            val offset = params.key ?: 0
            val media = loadMediaChunk(offset, params.loadSize, selection, selectionArgs)
            val nextKey = if (media.size < params.loadSize) {
                null
            } else {
                offset + params.loadSize
            }
            Log.i(TAG, "load: ")
            LoadResult.Page(
                data = media,
                prevKey = if (offset == 0) null else offset - 1,
                nextKey = nextKey
            )

        } catch (e: SecurityException) {
            Log.e(TAG, "load: ", e)
            LoadResult.Error(MediaLoadException.PermissionDenied(e))
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "load: ", e)
            LoadResult.Error(MediaLoadException.InvalidArguments(e))
        } catch (e: Exception) {
            Log.e(TAG, "load: ", e)
            LoadResult.Error(MediaLoadException.Generic(e))
        }

    }

    private suspend fun loadMediaChunk(
        offset: Int,
        limit: Int,
        selection: String,
        selectionArgs: Array<String>,
    ): List<MediaItem> {
        val collectionUri = when (mediaType) {
            MediaType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE
        )

        val cursor = contentResolver.query(
            collectionUri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_TAKEN} DESC "
        )
        Log.d(
            TAG,
            "loadMediaChunk() called with: offset = $offset, limit = $limit, ${cursor == null}"
        )
        return cursor?.use { parseCursor(it) } ?: emptyList()
    }

    private fun parseCursor(cursor: Cursor): List<MediaItem>? {
        val mediaItems = buildList {
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    when {
                        cursor.getString(mimeCol)?.startsWith("image/") == true ->
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                        else ->
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    },
                    id
                )

                add(
                    MediaItem(
                        id = id,
                        uri = uri,
                        name = cursor.getString(nameCol),
//                        dateTaken = cursor.getLong(dateCol),
//                        sizeBytes = cursor.getLong(sizeCol),
//                        mimeType = cursor.getString(mimeCol),
                        type = if (cursor.getString(mimeCol)?.startsWith("video/") == true) {
                            MediaType.VIDEO
                        } else {
                            MediaType.IMAGE
                        }
                    )
                )
            }
        }
        Log.d(TAG, "parseCursor() called ${mediaItems.size}")
        return mediaItems
    }

    sealed class MediaLoadException : Exception() {
        class PermissionDenied(cause: Throwable) : MediaLoadException()
        class InvalidArguments(cause: Throwable) : MediaLoadException()
        class Generic(cause: Throwable) : MediaLoadException()
    }
}