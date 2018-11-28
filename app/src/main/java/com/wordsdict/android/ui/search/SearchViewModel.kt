package com.wordsdict.android.ui.search

import androidx.lifecycle.*
import com.wordsdict.android.data.analytics.AnalyticsRepository
import com.wordsdict.android.data.prefs.Orientation
import com.wordsdict.android.data.prefs.PreferenceRepository
import com.wordsdict.android.data.repository.FirestoreUserSource
import com.wordsdict.android.data.repository.WordRepository
import com.wordsdict.android.data.repository.WordSource
import com.wordsdict.android.di.UserScope
import com.wordsdict.android.util.OrientationManager
import javax.inject.Inject

@UserScope
class SearchViewModel @Inject constructor(
        private val wordRepository: WordRepository,
        private val analyticsRepository: AnalyticsRepository,
        private val preferenceRepository: PreferenceRepository,
        private val orientationManager: OrientationManager
): ViewModel() {

    var searchInput: String = ""
        set(value) {
            if (value == field) return
            field = value
            searchInputData.value = value
        }

    private val searchInputData: MutableLiveData<String> = MutableLiveData()
    val searchResults: LiveData<List<WordSource>> = Transformations.switchMap(searchInputData) {
        if (it.isEmpty()) {
            wordRepository.getRecents(25L)
        } else {
            wordRepository.lookup(it)
        }
    }

    init {
        searchInputData.value = ""
    }

    private val wordId = MutableLiveData<String>()

    val firestoreUserSource: LiveData<FirestoreUserSource> = Transformations.switchMap(wordId) {
        wordRepository.getFirestoreUserSource(it)
    }

    fun setWordId(id: String) {
        if (wordId.value != id) {
            wordId.value = id
        }
    }

    fun logSearchWordEvent(id: String, word: WordSource) {
        analyticsRepository.logSearchWordEvent(searchInput, id, word::class.java.simpleName)
    }

    fun getOrientation(): LiveData<Pair<OrientationManager.Info, Orientation>> {
        return Transformations.map(orientationManager.getOrientation()) {
            Pair(it, Orientation.valueOf(preferenceRepository.orientationLock))
        }
    }

}

