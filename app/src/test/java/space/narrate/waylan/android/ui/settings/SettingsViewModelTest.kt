package space.narrate.waylan.android.ui.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenever
import space.narrate.waylan.android.CoroutinesTestRule
import space.narrate.waylan.android.FirestoreTestData
import space.narrate.waylan.android.R
import space.narrate.waylan.android.data.firestore.users.User
import space.narrate.waylan.android.data.repository.UserRepository
import space.narrate.waylan.android.valueBlocking
import java.util.*

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var settingsViewModel: SettingsViewModel
    private val userRepository = mock(UserRepository::class.java)

    private val user = MutableLiveData<User>()

    @Before
    fun setUp() {
        settingsViewModel = SettingsViewModel(userRepository)
    }

    @Test
    fun nonAnonymousUserOnPluginStateChanged_shouldUpdateBanner() {
        val registeredFreeUser = FirestoreTestData.registeredFreeValidUser
        user.value = registeredFreeUser
        whenever(userRepository.user).thenReturn(user)

        val bannerObserver = settingsViewModel.bannerModel

        assertThat(bannerObserver.valueBlocking.topButtonAction).isEqualTo(
            MwBannerAction.LAUNCH_PURCHASE_FLOW
        )

        user.value = registeredFreeUser.copy(
            merriamWebsterStarted = Date(),
            merriamWebsterPurchaseToken = "aslkfjwoeir23nasd"
        )

        val banner = bannerObserver.valueBlocking
        assertThat(banner.topButtonAction).isNull()
        assertThat(banner.labelRes).isEqualTo(R.string.settings_header_added_label)
        assertThat(banner.textRes).isEqualTo(R.string.settings_header_registered_subscribed_body)
    }
}