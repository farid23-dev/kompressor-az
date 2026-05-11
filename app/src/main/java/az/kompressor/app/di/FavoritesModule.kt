package az.kompressor.app.di

import android.content.Context
import androidx.room.Room
import az.kompressor.app.data.local.KompressorDatabase
import az.kompressor.app.data.local.dao.FavoriteCarDao
import az.kompressor.app.data.repository.FavoritesRepositoryImpl
import az.kompressor.app.domain.repository.FavoritesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FavoritesModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KompressorDatabase =
        Room.databaseBuilder(context, KompressorDatabase::class.java, "kompressor_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFavoriteCarDao(db: KompressorDatabase): FavoriteCarDao = db.favoriteCarDao()

    @Provides
    @Singleton
    fun provideFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository = impl
}
