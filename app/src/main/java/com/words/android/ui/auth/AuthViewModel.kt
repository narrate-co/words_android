package com.words.android.ui.auth

import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.words.android.util.FirebaseAuthWordErrorType
import javax.inject.Inject
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

class AuthViewModel @Inject constructor(): ViewModel() {


    suspend fun signUp(email: String, password: String, confirmPassword: String): FirebaseUser = suspendCoroutine { cont ->
        //TODO password and confirm password check, etc.
        //TODO use showErrorMessage() to show any validation errors

        val auth = FirebaseAuth.getInstance()
        when {
            password != confirmPassword -> cont.resumeWithFirebaseAuthException(FirebaseAuthWordErrorType.SIGN_UP_PASSWORDS_DONT_MATCH)
            auth.currentUser?.isAnonymous == true -> signUpByLinkingCredentials(cont, email, password)
            else -> signUpByCreatingEmptyAccount(cont, email, password)
        }
    }

    private fun signUpByLinkingCredentials(cont: Continuation<FirebaseUser>, email: String, password: String) {
        val credentials = EmailAuthProvider.getCredential(email, password)
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.linkWithCredential(credentials)?.addOnCompleteListener {
            if (it.isSuccessful) {
                val user = it.result?.user
                if (user != null) {
                    cont.resume(user)
                } else {
                    cont.resumeWithFirebaseAuthException(FirebaseAuthWordErrorType.SIGN_UP_FAILED, it)
                }
            } else {
                cont.resumeWithFirebaseAuthException(FirebaseAuthWordErrorType.SIGN_UP_FAILED, it)
            }
        } ?: cont.resumeWithFirebaseAuthException(FirebaseAuthWordErrorType.SIGN_UP_NO_CURRENT_USER)
    }

    private fun signUpByCreatingEmptyAccount(cont: Continuation<FirebaseUser>, email: String, password: String) {
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                val user = it.result?.user
                if (user != null) {
                    cont.resume(user)
                } else {
                    cont.resumeWithFirebaseAuthException(FirebaseAuthWordErrorType.SIGN_UP_FAILED)
                }
            } else {
                cont.resumeWithFirebaseAuthException(FirebaseAuthWordErrorType.SIGN_UP_FAILED, it)
            }
        }
    }

    suspend fun signUpAnonymously(): FirebaseUser = suspendCoroutine { cont ->
        val auth = FirebaseAuth.getInstance()
        auth.signInAnonymously().addOnCompleteListener {
            if (it.isSuccessful) {
                val user = auth.currentUser
                if (user != null) {
                    cont.resume(user)
                } else {
                    cont.resumeWithFirebaseAuthException(FirebaseAuthWordErrorType.ANON_UNKNOWN, it)
                }
            } else {
                cont.resumeWithFirebaseAuthException(FirebaseAuthWordErrorType.ANON_SIGN_UP_FAILED, it)
            }
        }
    }

    suspend fun logIn(email: String, password: String): FirebaseUser = suspendCoroutine {cont ->
        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        var user = auth.currentUser
                        if (user == null) {
                            cont.resumeWithFirebaseAuthException(FirebaseAuthWordErrorType.LOG_IN_UNKNOWN, it)
                        } else {
                            cont.resume(user)
                        }
                    } else {
                        cont.resumeWithFirebaseAuthException(FirebaseAuthWordErrorType.LOG_IN_FAILED, it)
                    }
                }
    }

    private fun <T> Continuation<T>.resumeWithFirebaseAuthException(type: FirebaseAuthWordErrorType, task: Task<AuthResult>? = null) {
        resumeWithException(task?.exception ?: type.exception)
    }

}
