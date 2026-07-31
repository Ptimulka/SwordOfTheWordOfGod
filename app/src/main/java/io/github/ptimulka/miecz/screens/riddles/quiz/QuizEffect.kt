package io.github.ptimulka.miecz.screens.riddles.quiz

sealed interface QuizEffect {
    data object Success : QuizEffect
}
