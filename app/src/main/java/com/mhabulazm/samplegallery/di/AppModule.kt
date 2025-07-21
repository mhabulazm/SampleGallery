package com.mhabulazm.samplegallery.di

import android.content.ContentResolver
import android.content.Context
import coil.ImageLoader
import com.mhabulazm.samplegallery.data.MediaRepositoryImpl
import com.mhabulazm.samplegallery.domain.GetAlbumsUseCase
import com.mhabulazm.samplegallery.domain.GetMediaUseCase
import com.mhabulazm.samplegallery.domain.MediaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    fun provideMediaRepository(contentResolver: ContentResolver): MediaRepository {
        return MediaRepositoryImpl(contentResolver)
    }

    // Provide Use Cases
    @Provides
    fun provideGetAlbumsUseCase(repository: MediaRepository): GetAlbumsUseCase {
        return GetAlbumsUseCase(repository)
    }

    @Provides
    fun provideGetMediaUseCase(repository: MediaRepository): GetMediaUseCase {
        return GetMediaUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
}