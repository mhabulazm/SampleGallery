package com.mhabulazm.samplegallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.paging.PagingSource
import com.mhabulazm.samplegallery.domain.MediaRepository
import com.mhabulazm.samplegallery.domain.entity.Album
import com.mhabulazm.samplegallery.domain.entity.MediaFilter
import com.mhabulazm.samplegallery.domain.entity.MediaItem
import com.mhabulazm.samplegallery.domain.entity.MediaType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MediaRepository {



    override fun getMediaPagingSource(
        mediaType: MediaType,
        mediaFilter: MediaFilter,
        albumId: String?,
    ): PagingSource<Int, MediaItem> {
        val mediaPagingSource = MediaPagingSource(contentResolver, mediaType, mediaFilter, albumId)
        Log.d("Gallery - Repo ", "getMediaPagingSource: $albumId ")
        return mediaPagingSource
    }

    override suspend fun getAlbums(mediaFilter: MediaFilter): List<Album> =
        withContext(ioDispatcher) {
            val albums = mutableListOf<Album>()

            albums.addAll(queryMediaAlbums(MediaStore.Images.Media.EXTERNAL_CONTENT_URI))

            albums.addAll(queryMediaAlbums(MediaStore.Video.Media.EXTERNAL_CONTENT_URI))

            albums.addAll(getSpecialAlbums())

            albums
        }

    private suspend fun queryMediaAlbums(
        collectionUri: Uri,
    ): List<Album> = withContext(ioDispatcher) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN
        )

        val albums = mutableMapOf<String, Album>()

        contentResolver.query(
            collectionUri,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_TAKEN} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketIdColumn)
                val bucketName = cursor.getString(bucketNameColumn)
                val mediaId = cursor.getLong(idColumn)

                val album = Album(
                    id = bucketId,
                    name = bucketName,
                    coverUri = ContentUris.withAppendedId(collectionUri, mediaId),
                    mediaCount = 1
                )
                albums[bucketId] = albums[bucketId]?.copy(
                    mediaCount = albums[bucketId]!!.mediaCount + 1
                ) ?: album
                Log.d("GALLERY", "queryMediaAlbums() called with: album $album")
            }
        }

        return@withContext albums.values.toList()
    }

    private suspend fun getSpecialAlbums(): List<Album> {
        val allImagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val allVideosUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        return listOf(
            Album(
                id = "ALL_IMAGES",
                name = "All Images",
                coverUri = getLatestMediaUri(allImagesUri),
                mediaCount = queryMediaCount(allImagesUri)
            ),
            Album(
                id = "ALL_VIDEOS",
                name = "All Videos",
                coverUri = getLatestMediaUri(allVideosUri),
                mediaCount = queryMediaCount(allVideosUri)
            ),
            Album(
                id = "CAMERA",
                name = "Camera",
                coverUri = getCameraAlbumCover(),
                mediaCount = queryCameraMediaCount()
            )
        )
    }

    private suspend fun getLatestMediaUri(collectionUri: Uri): Uri {
        //fixme sort
        val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC LIMIT 1"
        return withContext(ioDispatcher) {
            val projection =
                arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_TAKEN)
            contentResolver.query(
                collectionUri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    ContentUris.withAppendedId(
                        collectionUri,
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    )
                } else {
                    Uri.EMPTY
                }
            } ?: Uri.EMPTY
        }
    }

    private suspend fun queryMediaCount(collectionUri: Uri): Int {
        return withContext(ioDispatcher) {
            contentResolver.query(
                collectionUri,
                arrayOf(MediaStore.MediaColumns._ID),
                null,
                null,
                null
            )?.use { it.count } ?: 0
        }
    }

    private suspend fun queryCameraMediaCount(): Int {
        return withContext(ioDispatcher) {
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.BUCKET_DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf("Camera")

            val imageCount = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { it.count } ?: 0

            val videoCount = contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { it.count } ?: 0
            Log.d(
                "GALLERY - Camera MEDIA COUNT",
                "queryCameraMediaCount() called: $imageCount $videoCount"
            )
            imageCount + videoCount
        }
    }

    private suspend fun getCameraAlbumCover(): Uri {
        val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC LIMIT 1"
        return withContext(ioDispatcher) {
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.BUCKET_DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf("Camera")

            // Try images first
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return@withContext ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        cursor.getLong(0)
                    )
                }
            }

            // Fallback to videos
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return@withContext ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        cursor.getLong(0)
                    )
                }
            }

            Uri.EMPTY
        }
    }


}