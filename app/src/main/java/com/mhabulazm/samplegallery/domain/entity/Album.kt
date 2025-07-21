package com.mhabulazm.samplegallery.domain.entity

import android.net.Uri

data class Album (
    val id:String,
    val name:String,
    val coverUri: Uri,
    val mediaCount:Int
)