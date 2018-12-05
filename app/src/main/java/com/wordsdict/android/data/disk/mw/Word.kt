package com.wordsdict.android.data.disk.mw

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * A Merriam-Webster word
 *
 * @param id A unique identifier for each word. This is often the same as [word] except for
 *      when multiple entries exist for a word. For example, <i>quiet</i> might return
 *      3 entries from the Merriam-Webster API. In such a case the ids will often look like
 *      [quiet, quiet[1], quiet[2]]. When querying for a String, id should be ignored in favor
 *      of [word].
 * @param word The String value of the word as it appears in the dictionary
 * @param relatedWords A list of words (as they appear in the dictionary), which are slight
 *      variations of this [Word]. This is different from [suggestions] as [relatedWords] are
 *      returned for Merriam-Webster API requests that have non-empty responses
 * @param suggestions A list of words (as they appear in the dictionary), which are related to
 *      this [Word]. This field is usually only populated when a Merriam-Webster API request
 *      returns empty responses and instead return alternatives.
 *
 * @param uro //TODO this needs to be refactored into a list. See [Uro] for details
 */
@Entity(tableName = "mw_words")
data class Word(
        @PrimaryKey
        val id: String,
        val word: String,
        val subj: String,
        val phonetic: String,
        val sound: Sound,
        val pronunciation: String,
        val partOfSpeech: String,
        val etymology: String,
        val relatedWords: List<String>,
        var suggestions: List<String>,
        val uro: Uro
) {
        override fun equals(other: Any?): Boolean {
            if (other == null) return false
            if (other !is Word) return false
            if (this === other) return true

            return id == other.id &&
                    word == other.word &&
                    subj == other.subj &&
                    phonetic == other.phonetic &&
                    sound == other.sound &&
                    pronunciation == other.pronunciation &&
                    partOfSpeech == other.partOfSpeech &&
                    etymology == other.etymology
//            Uro not included!
        }

    override fun toString(): String {
        return "$id, $word, $subj, $phonetic, $sound, $pronunciation, $partOfSpeech, $etymology"
    }
}