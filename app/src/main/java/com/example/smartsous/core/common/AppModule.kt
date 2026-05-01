package com.example.smartsous.core.common

import android.content.Context
import androidx.room.Room
import com.example.smartsous.data.local.SmartSousDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// HILT DI MODULE

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): SmartSousDatabase =
        Room.databaseBuilder(ctx, SmartSousDatabase::class.java, "smartsous.db")
            .fallbackToDestructiveMigration() // chỉ dùng khi dev, bỏ khi release
            .build()

    @Provides fun provideRecipeDao(db: SmartSousDatabase) = db.recipeDao()
    @Provides fun provideIngredientDao(db: SmartSousDatabase) = db.ingredientDao()
    @Provides fun provideMealPlanDao(db: SmartSousDatabase) = db.mealPlanDao()
    @Provides fun provideChatMessageDao(db: SmartSousDatabase) = db.chatMessageDao()
}