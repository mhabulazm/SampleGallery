package com.mhabulazm.samplegallery.presentation.viewmodel

import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.mhabulazm.samplegallery.domain.GetAlbumsUseCase
import com.mhabulazm.samplegallery.domain.entity.Album
import com.mhabulazm.samplegallery.domain.entity.MediaFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val getAlbumsUseCase: GetAlbumsUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums

    private val _isLoading = savedStateHandle.getStateFlow("isLoading", false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var scrollPosition by savedStateHandle.saveable { mutableIntStateOf(0) }

    init {
        loadAlbums()
    }

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState

    private val _viewMode = MutableStateFlow(ViewMode.GRID)
    val viewMode: StateFlow<ViewMode> = _viewMode

    fun loadAlbums(filter: MediaFilter = MediaFilter()) {
        viewModelScope.launch {
            try {
                getAlbumsUseCase().collect { albums ->
                    _uiState.value = GalleryUiState.Success(albums)
                }
            } catch (e: Exception) {
                _uiState.value = GalleryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.GRID -> ViewMode.LIST
            ViewMode.LIST -> ViewMode.GRID
        }
    }

    sealed class GalleryUiState {
        data object Loading : GalleryUiState()
        data class Success(val albums: List<Album>) : GalleryUiState()
        data class Error(val message: String) : GalleryUiState()
    }

    enum class ViewMode {
        GRID, LIST
    }
}