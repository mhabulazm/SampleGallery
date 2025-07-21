package com.mhabulazm.samplegallery.domain.entity

import android.net.Uri

data class MediaItem(val id: Long, val uri: Uri, val name: String, val type: MediaType) {
}

enum class MediaType { IMAGE, VIDEO }