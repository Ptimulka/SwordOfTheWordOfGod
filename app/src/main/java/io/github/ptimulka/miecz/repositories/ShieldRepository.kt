package io.github.ptimulka.miecz.repositories

interface ShieldRepository {
    fun getShieldsCount(): Int
    fun setShieldsCount(count: Int)
    fun decreaseShields(): Int
    fun increaseShields(): Int
    fun getLastShieldUpdateTime(): Long
    fun refreshShields(): Int
    fun getTimeToNextShield(): Long
}
