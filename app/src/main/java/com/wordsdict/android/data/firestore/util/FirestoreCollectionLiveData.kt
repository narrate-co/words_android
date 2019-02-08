package com.wordsdict.android.data.firestore.util

import androidx.lifecycle.LiveData
import com.google.firebase.firestore.*
import kotlinx.coroutines.android.UI
import kotlinx.coroutines.launch



/**
 * A helper class to turn a Firestore [QuerySnapshot]
 * [EventListener] into a [LiveData] object
 */
class FirestoreCollectionLiveData<T>(
        private val query: Query,
        private val clazz: Class<T>
): LiveData<List<T>>() {

    companion object {
        private const val TAG = "FirestoreCollectionLiveData"
    }
    private var listenerRegistration: ListenerRegistration? = null

    private val eventListener =
            EventListener<QuerySnapshot> { querySnapshot, firebaseFirestoreException ->

        if (firebaseFirestoreException != null) {
            firebaseFirestoreException.printStackTrace()
        } else {
            // move parsing off the main thread
            launch(UI) {
                value = querySnapshot?.documents?.map { it.toObject(clazz)!! }
            }
        }
    }

    override fun onActive() {
        listenerRegistration = query.addSnapshotListener(MetadataChanges.INCLUDE, eventListener)
    }

    override fun onInactive() {
        listenerRegistration?.remove()
    }

}

