package io.github.ptimulka.miecz.repositories

import io.github.ptimulka.miecz.data.RiddleType

interface RiddlesOrderRepository {
    fun getRiddlesOrder(): List<List<RiddleType>>
}
