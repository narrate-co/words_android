package com.wordsdict.android.util

import android.annotation.SuppressLint
import android.os.Build
import android.text.Html
import android.text.Spanned


val isNougat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
val isOreo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O


val String.fromHtml: Spanned
    @SuppressLint("NewApi")
    get() = if (isNougat) Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY) else Html.fromHtml(this)

infix fun <T> Collection<T>.contentEquals(collection: Collection<T>?): Boolean
    = collection?.let { this.size == it.size && this.containsAll(collection) } ?: false

fun getScaleBetweenRange(value: Float, inputMin: Float, inputMax: Float, outputMin: Float, outputMax: Float): Float {
    if (value < inputMin) {
        return outputMin
    } else if (value > inputMax) {
        return outputMax
    }

    return outputMin * (1 - (value - inputMin) / (inputMax - inputMin)) + outputMax * ((value - inputMin) / (inputMax - inputMin))
}

