package com.rostomusic.app.di

import android.content.Context
import androidx.room.Room
import com.rostomusic.app.data.db.MIGRATION_1_2
import com.rostomusic.app.data.db.MIGRATION_2_3
import com.rostomusic.app.data.db.MIGRATION_3_4
import com.rostomusic.app.data.db.RostoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): RostoDatabase =
        Room.databaseBuilder(context, RostoDatabase::class.java, "rostomusic.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()

    /** Application-wide scope for work that must outlive a screen. */
    @Provides
    @Singleton
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}
