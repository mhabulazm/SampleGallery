package com.mhabulazm.samplegallery.domain

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.mhabulazm.samplegallery.domain.entity.MediaFilter
import com.mhabulazm.samplegallery.domain.entity.MediaItem
import com.mhabulazm.samplegallery.domain.entity.MediaType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import javax.inject.Inject

class GetMediaUseCase @Inject constructor(private val repo: MediaRepository) {
    companion object {
        private const val TAG = "GetMediaUseCase"
    }

    operator fun invoke(
        mediaType: MediaType, filter: MediaFilter, albumId: String?,
    ): Flow<PagingData<MediaItem>> {
        val pagingConfig =
            PagingConfig(pageSize = 60, prefetchDistance = 20, enablePlaceholders = false)
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = {
                try {
                    repo.getMediaPagingSource(mediaType, filter, albumId)
                } catch (e: Exception) {
                    Log.e(TAG, "invoke: ", e)
                    throw DomainMediaLoadException("Failed to create paging source", e)
                }
            }
        ).flow.catch { e ->
            Log.e(TAG, "invoke: ", e)
            when (e) {
                is SecurityException -> throw DomainMediaLoadException("Permission denied", e)
                is IllegalArgumentException -> throw DomainMediaLoadException(
                    "Invalid arguments",
                    e
                )

                else -> throw Exception("Unknown error", e)
            }
        }.retryWhen { cause, attempt ->
            if (cause !is DomainMediaLoadException || attempt >= 3) {
                false
            } else {
                delay(1000 * attempt)
                true
            }
        }
    }


}

