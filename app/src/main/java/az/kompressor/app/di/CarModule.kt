package az.kompressor.app.di

import az.kompressor.app.data.repository.CarRepositoryImpl
import az.kompressor.app.domain.repository.CarRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CarModule {

    @Binds @Singleton
    abstract fun bindCarRepository(impl: CarRepositoryImpl): CarRepository
}
