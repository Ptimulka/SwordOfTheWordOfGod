package io.github.ptimulka.miecz.repositories.game

import io.github.ptimulka.miecz.repositories.ShieldRepository
import io.github.ptimulka.miecz.repositories.core.UserProgressStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserShieldRepository @Inject constructor(
    private val store: UserProgressStore,
    private val currentTimeProvider: () -> Long
) : ShieldRepository {

    private companion object {
        const val MAX_SHIELDS = 5
        const val MIN_SHIELDS = 0
        const val SHIELD_REGEN_TIME_MS = 30 * 60 * 1000L
    }

    override fun getShieldsCount(): Int = store.latest.shieldsCount

    override fun setShieldsCount(count: Int) {
        val validated = count.coerceIn(MIN_SHIELDS, MAX_SHIELDS)
        store.update { it.toBuilder().setShieldsCount(validated).build() }
    }

    override fun decreaseShields(): Int {
        val current = getShieldsCount()
        if (current == MAX_SHIELDS) {
            store.update { it.toBuilder().setLastShieldUpdateTime(currentTimeProvider()).build() }
        }
        val next = (current - 1).coerceAtLeast(MIN_SHIELDS)
        setShieldsCount(next)
        return next
    }

    override fun increaseShields(): Int {
        val current = getShieldsCount()
        val next = (current + 1).coerceAtMost(MAX_SHIELDS)
        setShieldsCount(next)
        return next
    }

    override fun getLastShieldUpdateTime(): Long = store.latest.lastShieldUpdateTime

    override fun refreshShields(): Int {
        val currentShields = getShieldsCount()
        if (currentShields >= MAX_SHIELDS) return MAX_SHIELDS

        val lastUpdate = getLastShieldUpdateTime()
        val currentTime = currentTimeProvider()

        if (lastUpdate == 0L) {
            store.update { it.toBuilder().setLastShieldUpdateTime(currentTime).build() }
            return currentShields
        }

        val elapsed = currentTime - lastUpdate
        if (elapsed >= SHIELD_REGEN_TIME_MS) {
            val shieldsToAdd = (elapsed / SHIELD_REGEN_TIME_MS).toInt()
            val newCount = (currentShields + shieldsToAdd).coerceAtMost(MAX_SHIELDS)

            store.update { user ->
                val builder = user.toBuilder().setShieldsCount(newCount)
                if (newCount >= MAX_SHIELDS) {
                    builder.setLastShieldUpdateTime(0)
                } else {
                    builder.setLastShieldUpdateTime(lastUpdate + (shieldsToAdd * SHIELD_REGEN_TIME_MS))
                }
                builder.build()
            }
            return newCount
        }
        return currentShields
    }

    override fun getTimeToNextShield(): Long {
        if (getShieldsCount() >= MAX_SHIELDS) return 0L
        val lastUpdate = getLastShieldUpdateTime()
        if (lastUpdate == 0L) return 0L

        val elapsed = currentTimeProvider() - lastUpdate
        if (elapsed >= SHIELD_REGEN_TIME_MS) return 0L

        return (SHIELD_REGEN_TIME_MS - (elapsed % SHIELD_REGEN_TIME_MS)).coerceAtLeast(0L)
    }
}
