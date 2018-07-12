package com.words.android.data.firestore

import com.google.firebase.auth.FirebaseUser
import com.words.android.Config

data class User(
        val firebaseUser: FirebaseUser? = null,
        val isMerriamWebsterSubscriber: Boolean = false
)

