package io.github.ptimulka.miecz.repositories

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BestTimeEntry(val sectionId: Int, val sectionName: String, val timeMs: Long) : Parcelable

enum class ChosenPicture { NONE, DEFAULT, USER, IMPORTED }
