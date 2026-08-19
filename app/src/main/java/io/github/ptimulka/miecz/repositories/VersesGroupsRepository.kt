package io.github.ptimulka.miecz.repositories

import io.github.ptimulka.miecz.data.VerseGroup

interface VersesGroupsRepository {
    fun loadVerseGroups(): List<VerseGroup>
}
