package com.words.android

import androidx.lifecycle.*
import com.words.android.data.analytics.AnalyticsRepository
import com.words.android.data.analytics.NavigationMethod
import com.words.android.data.repository.WordRepository
import com.words.android.di.UserScope
import java.util.*
import javax.inject.Inject

@UserScope
class MainViewModel @Inject constructor(
        private val wordRepository: WordRepository,
        private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private var backStack: MutableLiveData<Stack<Navigator.HomeDestination>> = MutableLiveData()

    private var currentWordId = MutableLiveData<String>()

    val currentWord: LiveData<String> = currentWordId

    fun setCurrentWordId(id: String) {
        val sanitizedId = id.toLowerCase()
        currentWordId.value = sanitizedId
    }

    fun setCurrentWordFavorited(favorite: Boolean) {
        val id = currentWordId.value ?: return
        wordRepository.setFavorite(id, favorite)
    }

    fun setCurrentWordRecented() {
        val id = currentWordId.value ?: return
        wordRepository.setRecent(id)
    }

    fun getBackStack(): LiveData<Stack<Navigator.HomeDestination>> {
        return backStack
    }

    fun pushToBackStack(dest: Navigator.HomeDestination) {
        val stack = backStack.value ?: Stack()
        stack.push(dest)
        backStack.value = stack
    }

    fun popBackStack(unconsumedNavigationMethod: NavigationMethod?) {
        val stack = backStack.value

        if (unconsumedNavigationMethod != null) {
            // this nav is coming from either a nav_icon click or a drag_dismiss
            analyticsRepository.logNavigateBackEvent(stack.toString(), unconsumedNavigationMethod)
        } else {
            analyticsRepository.logNavigateBackEvent(stack.toString(), NavigationMethod.BACK_BUTTON)
        }


        stack?.pop()
        if (stack != null) backStack.value = stack
    }

}
