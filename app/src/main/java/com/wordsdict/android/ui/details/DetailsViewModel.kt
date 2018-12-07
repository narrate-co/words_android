package com.wordsdict.android.ui.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.wordsdict.android.data.repository.*
import com.wordsdict.android.di.UserScope
import javax.inject.Inject

/**
 * ViewModel for [DetailsFragment]
 */
@UserScope
class DetailsViewModel @Inject constructor(
        private val wordRepository: WordRepository
): ViewModel() {

    // The current word being displayed (as it appears in the dictionary)
    private var _word = MutableLiveData<String>()

    /** LiveData object for [_word]'s [WordPropertiesSource] */
    val wordPropertiesSource: LiveData<WordPropertiesSource> = Transformations.switchMap(_word) {
        wordRepository.getWordPropertiesSource(it)
    }

    /** LiveData object for [_word]'s [WordsetSource] */
    val wordsetSource: LiveData<WordsetSource?> = Transformations.switchMap(_word) {
        wordRepository.getWordsetSource(it)
    }

    /** LiveData object for [_word]'s [FirestoreUserSource] */
    val firestoreUserSource: LiveData<FirestoreUserSource> = Transformations.switchMap(_word) {
        wordRepository.getFirestoreUserSource(it)
    }

    /** LiveData object for [_word]'s [FirestoreGlobalSource] */
    val firestoreGlobalSource: LiveData<FirestoreGlobalSource> = Transformations.switchMap(_word) {
        wordRepository.getFirestoreGlobalSource(it)
    }

    /** LiveData object for [_word]'s [MerriamWebsterSource] */
    val merriamWebsterSource: LiveData<MerriamWebsterSource> = Transformations.switchMap(_word) {
        wordRepository.getMerriamWebsterSource(it)
    }

    /**
     * Set the current word (as it appears in the dictionary) that is to be displayed
     */
    fun setWord(word: String) {
        if (_word.value != word) {
            _word.value = word
        }
    }

}