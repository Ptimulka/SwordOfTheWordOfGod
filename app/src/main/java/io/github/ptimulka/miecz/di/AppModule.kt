package io.github.ptimulka.miecz.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.helpers.AndroidNotificationScheduler
import io.github.ptimulka.miecz.helpers.NotificationScheduler
import io.github.ptimulka.miecz.repositories.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton
import javax.inject.Named

import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import io.github.ptimulka.miecz.data.UserProgress

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserProgressDataStore(@ApplicationContext context: Context): DataStore<UserProgress> {
        return context.userProgressDataStore
    }

    private val Context.userProgressDataStore: DataStore<UserProgress> by dataStore(
        fileName = "user_progress.pb",
        serializer = UserProgressSerializer
    )

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepository(context)
    }

    @Provides
    @Singleton
    fun provideNotificationScheduler(@ApplicationContext context: Context): NotificationScheduler {
        return AndroidNotificationScheduler(context)
    }

    @Provides
    fun provideTimeProvider(): () -> Long = { System.currentTimeMillis() }

    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Named("appVersion")
    fun provideAppVersion(@ApplicationContext context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    @Provides
    @Named("assetList")
    fun provideAssetList(@ApplicationContext context: Context): Set<String> {
        return context.assets.list("default_mnemonics")?.toSet() ?: emptySet()
    }

    @Provides
    @Named("randomVerseTitle")
    fun provideRandomVerseTitle(@ApplicationContext context: Context): String {
        return context.getString(R.string.random_verse)
    }

    @Provides
    @Named("reviewSectionName")
    fun provideReviewSectionName(@ApplicationContext context: Context): String {
        return context.getString(R.string.repeat_level_name)
    }
}
