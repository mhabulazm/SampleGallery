package com.mhabulazm.samplegallery.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mhabulazm.samplegallery.domain.GetMediaUseCase
import com.mhabulazm.samplegallery.domain.entity.MediaFilter
import com.mhabulazm.samplegallery.domain.entity.MediaItem
import com.mhabulazm.samplegallery.domain.entity.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val getMediaUseCase: GetMediaUseCase,
    private val saveState: SavedStateHandle,
) :
    ViewModel() {
    private val _viewMode = MutableStateFlow(ViewMode.GRID)
    val viewMode: StateFlow<ViewMode> = _viewMode

    fun getMedia(albumId: String?): Flow<PagingData<MediaItem>> {
        Log.d(TAG, "getMedia() called with: albumId = $albumId")
        return getMediaUseCase(
            mediaType = MediaType.IMAGE,
            albumId = albumId,
            filter = MediaFilter()
        ).onEach {
            Log.d(TAG, "getMedia() called , $it")
        }.catch { e ->
            Log.e(TAG, "getMedia: ", e)
        }.cachedIn(viewModelScope)
    }

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.GRID -> ViewMode.LIST
            ViewMode.LIST -> ViewMode.GRID
        }
    }

    fun saveScrollPosition(position: Int) {
        saveState["scrollPosition"] = position

    }

    enum class ViewMode {
        GRID, LIST
    }

    companion object {
        private const val TAG = "Gallery Album Details ViewModel"

    }
}