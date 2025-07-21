package com.mhabulazm.samplegallery.domain

import com.mhabulazm.samplegallery.domain.entity.Album
import com.mhabulazm.samplegallery.domain.entity.MediaFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetAlbumsUseCase @Inject constructor(
    private val repository: MediaRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    operator fun invoke(mediaFilter: MediaFilter = MediaFilter()): Flow<List<Album>> {
        return flow {
            try {
                emit(emptyList<Album>())
                val albums = repository.getAlbums(mediaFilter)
                emit(processAlbums(albums))
            } catch (e: Exception) {
                throw AlbumLoadException("Failed to load albums", e)
            }
        }.flowOn(dispatcher).distinctUntilChanged()
    }

    private fun processAlbums(albums: List<Album>): List<Album> {
        return albums
            .filterNot { it.name.contains("cache", ignoreCase = true) }
            .filterNot { it.mediaCount == 0 }
            .sortedByDescending { it.mediaCount }
            .takeIf { it.isNotEmpty() }
            ?: throw NoAlbumsFoundException()
    }
}

class AlbumLoadException(message: String, cause: Throwable) : Exception(message, cause)
class NoAlbumsFoundException : Exception("No Albums found with the given Id")