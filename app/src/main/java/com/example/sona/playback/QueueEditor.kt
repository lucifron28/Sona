package com.example.sona.playback

fun <T> List<T>.moveItem(fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return this

    return toMutableList().apply {
        val item = removeAt(fromIndex)
        add(toIndex, item)
    }
}

fun <T> List<T>.removeIndex(index: Int): List<T> {
    if (index !in indices) return this

    return toMutableList().apply {
        removeAt(index)
    }
}
