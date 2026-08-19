package io.github.ptimulka.miecz.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.ptimulka.miecz.repositories.*
import io.github.ptimulka.miecz.repositories.content.*
import io.github.ptimulka.miecz.repositories.game.*
import io.github.ptimulka.miecz.repositories.meta.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProgressRepository(impl: UserProgressRepository): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindShieldRepository(impl: UserShieldRepository): ShieldRepository

    @Binds
    @Singleton
    abstract fun bindStreakRepository(impl: UserStreakRepository): StreakRepository

    @Binds
    @Singleton
    abstract fun bindProgressionRepository(impl: UserProgressionRepository): ProgressionRepository

    @Binds
    @Singleton
    abstract fun bindRetentionRepository(impl: UserRetentionRepository): RetentionRepository

    @Binds
    @Singleton
    abstract fun bindMnemonicChoiceRepository(impl: UserMnemonicChoiceRepository): MnemonicChoiceRepository

    @Binds
    @Singleton
    abstract fun bindCustomSectionRepository(impl: UserCustomSectionRepository): CustomSectionRepository

    @Binds
    @Singleton
    abstract fun bindAchievementRepository(impl: UserAchievementRepository): AchievementRepository

    @Binds
    @Singleton
    abstract fun bindSectionRepository(impl: UserSectionRepository): SectionRepository

    @Binds
    @Singleton
    abstract fun bindVersesGroupsRepository(impl: UserVersesGroupsRepository): VersesGroupsRepository

    @Binds
    @Singleton
    abstract fun bindMnemonicRepository(impl: UserMnemonicPicturesRepository): MnemonicRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: UserSettingsRepository): SettingsRepository
    
    @Binds
    @Singleton
    abstract fun bindRiddlesOrderRepository(impl: UserRiddlesOrderRepository): RiddlesOrderRepository
}
