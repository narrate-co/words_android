package com.words.android.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.words.android.data.disk.wordset.WordAndMeanings
import com.words.android.data.disk.AppDatabase
import com.words.android.data.disk.mw.PermissiveWordsDefinitions
import com.words.android.data.firestore.FirestoreStore
import com.words.android.data.firestore.users.UserWord
import com.words.android.data.firestore.words.GlobalWord
import com.words.android.data.mw.MerriamWebsterStore
import com.words.android.data.spell.SymSpellStore
import com.words.android.util.LiveDataHelper
import com.words.android.util.MergedLiveData
import kotlinx.coroutines.launch

class WordRepository(
        private val db: AppDatabase,
        private val firestoreStore: FirestoreStore?,
        private val merriamWebsterStore: MerriamWebsterStore?,
        private val symSpellStore: SymSpellStore
) {

    fun filterWords(input: String): LiveData<List<WordSource>> {
        return Transformations.map(db.wordDao().load("$input%")) { word ->
            word.map { SimpleWordSource(it) }
        }
    }

    fun lookup(input: String): LiveData<List<WordSource>> {
        val suggSource = Transformations.map(symSpellStore.lookupLive(input)) { list ->
            list.map { SuggestSource(it) }
        }

        return MergedLiveData(filterWords(input), suggSource) { d1, d2 ->
            //TODO deduplicate/sort smartly. Make WordSource comparable?
            (d1 + d2)
        }
    }

    //TODO write a method that returns a WordSourceGroup
    //TODO on restoreInstanceState, the last value gets passed to the observer, meaning only
    //TODO the latest source will get passed again, leaving all else empty
    fun getWordSources(id: String): LiveData<WordSource> {
        println("WordRepository::getWordSources - id = $id")
        val mediatorLiveData = MediatorLiveData<WordSource>()

        // Word Properties
        mediatorLiveData.addSource(getWordProperties(id)) {
            if (it != null) mediatorLiveData.value = WordPropertiesSource(it)
        }

        // Wordset
        mediatorLiveData.addSource(getWordAndMeanings(id)) {
            if (it != null) mediatorLiveData.value = WordsetSource(it)
        }

        // Firestore User Word
        mediatorLiveData.addSource(getUserWord(id)) {
            if (it != null) mediatorLiveData.value = FirestoreUserSource(it)
        }

        // Firestore Global Word
        mediatorLiveData.addSource(getGlobalWord(id)) {
            if (it != null) mediatorLiveData.value = FirestoreGlobalSource(it)
        }

        mediatorLiveData.addSource(getMerriamWebsterWordAndDefinitions(id)) {
            if (it != null) mediatorLiveData.value = MerriamWebsterSource(it)
        }

        return mediatorLiveData
    }

    fun getWordPropertiesSource(id: String): LiveData<WordPropertiesSource> {
        return Transformations.map(getWordProperties(id)) {
            WordPropertiesSource(it)
        }
    }

    private fun getWordProperties(word: String): LiveData<WordProperties> {
        val data = MutableLiveData<WordProperties>()
        data.value = WordProperties(word, word)
        return data
    }

    fun getWordsetSource(id: String): LiveData<WordsetSource?> {
        return Transformations.map(getWordAndMeanings(id)) {
            if (it != null) WordsetSource(it) else null
        }
    }

    private fun getWordAndMeanings(word: String): LiveData<WordAndMeanings> =
            if (word.isNotBlank()) db.wordDao().getWordAndMeanings(word) else LiveDataHelper.empty()

   fun getFirestoreUserSource(id: String): LiveData<FirestoreUserSource> {
        return Transformations.map(getUserWord(id)) {
            FirestoreUserSource(it)
        }
    }

    private fun getUserWord(id: String): LiveData<UserWord> {
        return if (id.isNotBlank()) firestoreStore?.getUserWordLive(id) ?: LiveDataHelper.empty() else LiveDataHelper.empty()
    }

    fun getFirestoreGlobalSource(id: String): LiveData<FirestoreGlobalSource> {
        return Transformations.map(getGlobalWord(id)) {
            FirestoreGlobalSource(it)
        }
    }

    private fun getGlobalWord(id: String): LiveData<GlobalWord> {
        return if (id.isNotBlank()) firestoreStore?.getGlobalWordLive(id) ?: LiveDataHelper.empty() else LiveDataHelper.empty()
    }

    fun getMerriamWebsterSource(id: String): LiveData<MerriamWebsterSource> {
        return Transformations.map(getMerriamWebsterWordAndDefinitions(id)) {
            MerriamWebsterSource(it)
        }
    }

    private fun getMerriamWebsterWordAndDefinitions(id: String): LiveData<PermissiveWordsDefinitions> {
        if (id.isBlank() || merriamWebsterStore == null || firestoreStore == null) return LiveDataHelper.empty()

        return MergedLiveData(
                merriamWebsterStore.getWord(id),
                firestoreStore.getUserLive()) { d1, d2 ->
            PermissiveWordsDefinitions(d2, d1)
        }
    }

    fun getTrending(limit: Long? = null): LiveData<List<WordSource>> {
        if (firestoreStore == null) return LiveDataHelper.empty()
        return Transformations.map(firestoreStore.getTrending(limit)) { globalWords ->
            globalWords.map { FirestoreGlobalSource(it) }
        }
    }

    fun getFavorites(limit: Long? = null): LiveData<List<WordSource>> {
        if (firestoreStore == null) return LiveDataHelper.empty()
        return Transformations.map(firestoreStore.getFavorites(limit)) { userWords ->
            userWords.map { FirestoreUserSource(it) }
        }
    }

    fun setFavorite(id: String, favorite: Boolean) {
        if (id.isBlank()) return

        launch {
            firestoreStore?.setFavorite(id, favorite)
        }
    }

    fun getRecents(limit: Long? = null): LiveData<List<WordSource>> {
        if (firestoreStore == null) return LiveDataHelper.empty()
        return Transformations.map(firestoreStore.getRecents(limit)) { userWords ->
            userWords.map { FirestoreUserSource(it) }
        }
    }

    fun setRecent(id: String) {
        if (id.isBlank()) return

        launch {
            firestoreStore?.setRecent(id)
        }
    }

}

