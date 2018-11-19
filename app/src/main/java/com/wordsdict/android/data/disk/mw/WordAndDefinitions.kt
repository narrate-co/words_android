package com.wordsdict.android.data.disk.mw

import androidx.room.Embedded
import androidx.room.Relation

data class WordAndDefinitions(
        @Embedded
        var word: Word? = null,
        @Relation(parentColumn = "id", entityColumn = "parentId")
        var definitions: List<Definition> = ArrayList()
)
