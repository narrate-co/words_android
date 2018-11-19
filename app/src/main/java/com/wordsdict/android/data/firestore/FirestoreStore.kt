package com.wordsdict.android.data.firestore

import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.wordsdict.android.data.DataOwners
import com.wordsdict.android.data.disk.AppDatabase
import com.wordsdict.android.data.firestore.users.*
import com.wordsdict.android.data.firestore.util.FirebaseFirestoreNotFoundException
import com.wordsdict.android.data.firestore.util.liveData
import com.wordsdict.android.data.firestore.words.GlobalWord
import com.wordsdict.android.util.isMoreThanOneMinuteAgo
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirestoreStore(
        private val firestore: FirebaseFirestore,
        private val db: AppDatabase,
        private val firestoreUser: FirebaseUser
) {

    companion object {
        private const val TAG = "FirestoreStore"
    }



    fun getGlobalWordLive(id: String): LiveData<GlobalWord> {
        return firestore.words
                .document(id)
                .liveData(GlobalWord::class.java)
    }

    fun getUserWordLive(id: String): LiveData<UserWord> {
        return firestore.userWords(firestoreUser.uid)
                .document(id)
                .liveData(UserWord::class.java)
    }

    fun getUserLive(): LiveData<User> {
        return firestore.users
                .document(firestoreUser.uid)
                .liveData(User::class.java)
    }

    suspend fun getUser(): User = suspendCoroutine { cont ->
        firestore.users.document(firestoreUser.uid).get()
                .addOnFailureListener {
                    cont.resumeWithException(it)
                }
                .addOnSuccessListener {
                    if (it.exists()) {
                        cont.resume(it.toObject(User::class.java)!!)
                    } else {
                        cont.resumeWithException(FirebaseFirestoreNotFoundException(firestoreUser.uid))
                    }
                }
    }

    private suspend fun getUserWord(id: String, createIfDoesNotExist: Boolean): UserWord = suspendCoroutine { cont ->
        firestore.userWords(firestoreUser.uid).document(id).get()
                .addOnFailureListener {
                    when ((it as FirebaseFirestoreException).code) {
                        FirebaseFirestoreException.Code.UNAVAILABLE -> {
                            if (createIfDoesNotExist) {
                                launch {
                                    val newUserWord = newUserWord(id).await()
                                    if (newUserWord != null) {
                                        cont.resume(newUserWord)
                                    } else {
                                        cont.resumeWithException(FirebaseFirestoreException("Unable to create new UserWord", FirebaseFirestoreException.Code.UNKNOWN))
                                    }
                                }
                            } else {
                                cont.resumeWithException(it)
                            }
                        }
                        else -> cont.resumeWithException(it)
                    }
                }
                .addOnSuccessListener {
                    if (it.exists()) {
                        cont.resume(it.toObject(UserWord::class.java)!!)
                    } else {
                        if (createIfDoesNotExist) {
                            launch {
                                val newUserWord = newUserWord(id).await()
                                if (newUserWord != null) {
                                    cont.resume(newUserWord)
                                } else {
                                    cont.resumeWithException(FirebaseFirestoreException("Unable to create new UserWord", FirebaseFirestoreException.Code.UNKNOWN))
                                }
                            }
                        } else {
                            cont.resumeWithException(FirebaseFirestoreNotFoundException(id))
                        }
                    }
                }
    }


    private fun newUserWord(id: String): Deferred<UserWord?> = async {
        //get word from db.
        val word = db.wordDao().get(id)
        //get meanings from db.
        val meanings = db.meaningDao().get(id)

        if (word == null) {
            null
        } else {

            val partOfSpeech: Map<String, String> = meanings?.map { it.partOfSpeech to DataOwners.WORDSET.name }?.distinct()?.toMap() ?: mapOf()
            val defs: Map<String, String> = meanings?.map { it.def to DataOwners.WORDSET.name }?.distinct()?.toMap() ?: mapOf()
            val synonyms: Map<String, String> = meanings?.flatMap { it.synonyms }?.map { it.synonym to DataOwners.WORDSET.name }?.distinct()?.toMap() ?: mapOf()
            val labels: Map<String, String> = meanings?.flatMap { it.labels }?.map { it.name to DataOwners.WORDSET.name }?.distinct()?.toMap() ?: mapOf()


            val userWord = UserWord(
                    id,
                    word.word,
                    Date(),
                    Date(),
                    mutableMapOf(),
                    partOfSpeech.toMutableMap(),
                    defs.toMutableMap(),
                    synonyms.toMutableMap(),
                    labels.toMutableMap())

            userWord
        }
    }

    fun getTrending(limit: Long? = null): LiveData<List<GlobalWord>> {
        val query = firestore.words
                .orderBy("totalViewCount", Query.Direction.DESCENDING)
                .limit(limit ?: 25)

        return query.liveData(GlobalWord::class.java)
    }

    //get all favorites for firestoreUser
    fun getFavorites(limit: Long? = null): LiveData<List<UserWord>> {
        val query = firestore.userWords(firestoreUser.uid)
                .whereEqualTo("types.${UserWordType.FAVORITED.name}", true)
                .orderBy("modified", Query.Direction.DESCENDING)
                .limit(limit ?: 25)

        return query.liveData(UserWord::class.java)
    }

    //favorite a word for firestoreUser
    suspend fun setFavorite(id: String, favorite: Boolean) {
        try {
            val userWord = getUserWord(id, favorite)
            if (favorite) {
                userWord.types[UserWordType.FAVORITED.name] = true
            } else {
                userWord.types.remove(UserWordType.FAVORITED.name)
            }
            userWord.types[UserWordType.RECENT.name] = true
            setUserWord(userWord)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getRecents(limit: Long? = null): LiveData<List<UserWord>> {
        val query = firestore.userWords(firestoreUser.uid)
                .whereEqualTo("types.${UserWordType.RECENT.name}", true)
                .orderBy("modified", Query.Direction.DESCENDING)
                .limit(limit ?: 25)

        return query.liveData(UserWord::class.java)
    }

    suspend fun setRecent(id: String) {
        try {
            val userWord = getUserWord(id, true)
            if (!userWord.types.containsKey(UserWordType.RECENT.name) || userWord.modified.isMoreThanOneMinuteAgo) {
                userWord.types[UserWordType.RECENT.name] = true
                userWord.modified = Date()
                userWord.totalViewCount = userWord.totalViewCount + 1
                setUserWord(userWord)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setUserWord(userWord: UserWord) {
        firestore.userWords(firestoreUser.uid).document(userWord.id).set(userWord)
                .addOnFailureListener {
                    it.printStackTrace()
                    //TODO report error?
                }
                .addOnSuccessListener {
                    //TODO show success?
                }
    }

    suspend fun setUserMerriamWebsterState(state: PluginState) {
        try {
            val user = getUser()
            user.merriamWebsterStarted = state.started
            user.isMerriamWebsterSubscriber = state is PluginState.Purchased
            setUser(user)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setUser(user: User) {
        firestore.users.document(user.uid).set(user)
                .addOnFailureListener {
                    it.printStackTrace()
                }
                .addOnSuccessListener {
                    //TODO show success?
                }
    }


    //add meaning

    //edit meaning (synonyms, examples, part of speech, labels

    //delete meaning
}