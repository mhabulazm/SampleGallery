package com.mhabulazm.samplegallery.domain.entity

data class MediaFilter(
    val minSizeBytes: Long = 0L,
    val maxSizeBytes: Long = Long.MAX_VALUE,
    val afterDate: Long = 0L,
    val beforeDate: Long = Long.MAX_VALUE,
    val mimeTypes: Set<String> = setOf("image/*", "video/*"),
    )