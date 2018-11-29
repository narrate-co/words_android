package com.wordsdict.android.ui.search

import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import java.lang.StringBuilder
import java.util.*

/**
 * A class that holds a set number of items and maintains chronological list from
 * most recently push to least recently pushed
 */
class TrimmedStack<T>(private var array: MutableList<T?>): Iterable<T?> {

    constructor(capacity: Int, init: (index: Int) -> T?) : this(MutableList(capacity, init))

    val capacity: Int = array.size

    fun push(t: T) {
        Collections.rotate(array, 1)
        array[0] = t
        println("TrimmedStack::push. new ")
    }

    fun peek(): T? {
        return array[0]
    }

    inner class TrimmedStackIterator(startFrom: Int = 0): Iterator<T?> {
        private var current: Int = startFrom

        override fun hasNext(): Boolean {
            return array.size > current
        }

        override fun next(): T? {
            val i = current
            current++
            return array[i]
        }
    }

    override fun iterator(): Iterator<T?> {
        return TrimmedStackIterator()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.insert(0, "]")
        forEachIndexed { index, t ->
            if (index == 0) {
                sb.insert(0, t.toString())
            } else {
                sb.insert(0, "$t, ")
            }
        }
        sb.insert(0, "[")
        return sb.toString()
    }
}

fun <T> trimmedStackOf(vararg elements: T): TrimmedStack<T> =
        if (elements.size > 0) {
            TrimmedStack<T>(elements.asList().toMutableList())
        } else {
            TrimmedStack(0) { null }
        }

fun <T> emptyTrimmedStack(capacity: Int): TrimmedStack<T> = TrimmedStack(capacity) { null }

/**
 * @param pattern The pattern to match in chronological order from least recent to most recent
 *
 * pattern -> portrait, landscape, reverse portrait
 * matching stack -> [x,x,x portrait, landscape, reverse portrait]
 */
fun <T> TrimmedStack<T>.hasPattern(pattern: List<T>): Boolean {
    if (capacity < pattern.size) return false

    var matches = true

    forEachIndexed { index, t ->
        //only go up to the last index of pattern
        if (index > pattern.lastIndex) return@forEachIndexed

        val patternIndex = pattern.lastIndex - index
        if (t != pattern[patternIndex]) {
            matches = false
            return@forEachIndexed
        }
    }

    return matches
}