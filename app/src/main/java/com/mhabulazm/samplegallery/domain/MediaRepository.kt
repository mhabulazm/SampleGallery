package com.mhabulazm.samplegallery.domain

import androidx.paging.PagingSource
import com.mhabulazm.samplegallery.domain.entity.Album
import com.mhabulazm.samplegallery.domain.entity.MediaFilter
import com.mhabulazm.samplegallery.domain.entity.MediaItem
import com.mhabulazm.samplegallery.domain.entity.MediaType

interface MediaRepository {

    fun getMediaPagingSource(
        mediaType: MediaType,
        mediaFilter: MediaFilter,
        albumId: String? = null,
    ): PagingSource<Int, MediaItem>

    suspend fun getAlbums(mediaFilter: MediaFilter): List<Album>
}