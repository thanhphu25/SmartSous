package com.example.smartsous.core.common

import com.example.smartsous.data.repository.ChatRepositoryImpl
import com.example.smartsous.data.repository.PantryRepositoryImpl
import com.example.smartsous.data.repository.RecipeRepositoryImpl
import com.example.smartsous.domain.repository.IChatRepository
import com.example.smartsous.domain.repository.IPantryRepository
import com.example.smartsous.domain.repository.IRecipeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // Khi ai inject IChatRepository → tạo ChatRepositoryImpl
    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): IChatRepository

    // Các repository khác sẽ thêm vào đây sau
    @Binds @Singleton
    abstract fun bindRecipeRepository(impl: RecipeRepositoryImpl): IRecipeRepository

    @Binds @Singleton
    abstract fun bindPantryRepository(impl: PantryRepositoryImpl): IPantryRepository
}