package io.github.ptimulka.miecz.screens.riddles.multi_quiz

sealed interface MultiQuizEffect {
    data object Success : MultiQuizEffect
}
