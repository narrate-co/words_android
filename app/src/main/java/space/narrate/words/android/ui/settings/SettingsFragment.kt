package space.narrate.words.android.ui.settings

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatImageButton
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import space.narrate.words.android.*
import space.narrate.words.android.billing.BillingConfig
import space.narrate.words.android.billing.BillingManager
import space.narrate.words.android.ui.common.BaseUserFragment
import space.narrate.words.android.util.configError
import space.narrate.words.android.data.prefs.Orientation
import space.narrate.words.android.ui.dialog.NightModeDialog
import space.narrate.words.android.ui.dialog.OrientationDialog
import space.narrate.words.android.util.gone
import space.narrate.words.android.util.visible
import space.narrate.words.android.util.widget.BannerCardView
import space.narrate.words.android.util.widget.CheckPreferenceView
import javax.inject.Inject

/**
 * A [Fragment] that displays the main settings screen with an account banner (plugins and
 * important user prompts) and the most common settings like orientation lock, night mode,
 * sign out as well as subsequent settings views like about, contact and developer options
 *
 * [R.id.banner] Should show either a prompt to sign up/log in or publish the availability
 *  and status of the user's Merriam-Webster plugin
 * [R.id.night_mode_preference] Allows the user to switch between a light theme, a night theme or optionally
 * allowing the user to have these set by time of day or the OS's settings
 * [R.id.orientation_preference] Allows the user to explicitly lock the app's orientation
 * [R.id.sign_out_preference] Should only show for registered users and allows the user to log out and sign in
 *  with different credentials or create a new account
 * [R.id.about_preference] Leads to [AboutFragment]
 * [R.id.contact_preference] Calls [Navigator.launchEmail]
 * [R.id.developer_preference] Leads to [DeveloperSettingsFragment] and is only shown for debug builds
 */
class SettingsFragment : BaseUserFragment(), BannerCardView.Listener {

    @Inject
    lateinit var billingManger: BillingManager

    private lateinit var settingsCoordinatorLayout: CoordinatorLayout
    private lateinit var navigationIcon: AppCompatImageButton
    private lateinit var bannerCardView: BannerCardView
    private lateinit var nightModePreference: CheckPreferenceView
    private lateinit var orientationPreference: CheckPreferenceView
    private lateinit var signOutPreference: CheckPreferenceView
    private lateinit var aboutPreference: CheckPreferenceView
    private lateinit var contactPreference: CheckPreferenceView
    private lateinit var developerPreference: CheckPreferenceView

    // This SettingsFragment's own ViewModel
    private val viewModel by lazy {
        ViewModelProviders
            .of(this, viewModelFactory)
            .get(SettingsViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingsCoordinatorLayout = view.findViewById(R.id.settings_coordinator)
        navigationIcon = view.findViewById(R.id.navigation_icon)
        bannerCardView = view.findViewById(R.id.banner)
        nightModePreference = view.findViewById(R.id.night_mode_preference)
        orientationPreference = view.findViewById(R.id.orientation_preference)
        signOutPreference = view.findViewById(R.id.sign_out_preference)
        aboutPreference = view.findViewById(R.id.about_preference)
        contactPreference = view.findViewById(R.id.contact_preference)
        developerPreference = view.findViewById(R.id.developer_preference)

        navigationIcon.setOnClickListener { activity?.onBackPressed() }

        viewModel.shouldLaunchAuth.observe(this, Observer { event ->
            event.getUnhandledContent()?.let { Navigator.launchAuth(requireContext(), it) }
        })

        viewModel.shouldLaunchMwPurchaseFlow.observe(this, Observer { event ->
            event.getUnhandledContent()?.let {
                billingManger.initiatePurchaseFlow(activity!!, BillingConfig.SKU_MERRIAM_WEBSTER)
            }
        })

        setUpBanner()

        setUpNightMode()

        setUpOrientation()

        // Sign out preference
        signOutPreference.setOnClickListener { viewModel.onSignOutClicked() }

        // About preference
        aboutPreference.setOnClickListener { (requireActivity() as SettingsActivity).showAbout() }

        // Contact preference
        // If debug, there will be a developer settings item after this preference. Show divider
        contactPreference.setShowDivider(BuildConfig.DEBUG)
        contactPreference.setOnClickListener {
            try {
                Navigator.launchEmail(context!!, SUPPORT_EMAIL_ADDRESS, getString(R.string.settings_email_compose_subject))
            } catch (e: ActivityNotFoundException) {
                Snackbar.make(
                    settingsCoordinatorLayout,
                    getString(R.string.settings_email_compose_no_client_error),
                    Snackbar.LENGTH_SHORT
                )
                    .configError(context!!, false)
                    .show()
            }
        }

        // Developer settings preference, only shown if this is a debug build
        // TODO further lock this down. Possibly by user?
        developerPreference.visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
        developerPreference.setOnClickListener {
            (requireActivity() as SettingsActivity).showDeveloperSettings()
        }
    }

    private fun setUpBanner() {
        bannerCardView.setLisenter(this)
        viewModel.bannerModel.observe(this, Observer { model ->
            bannerCardView
                .setText(model.textRes)
                .setLabel(MwBannerModel.getConcatenatedLabel(
                    requireContext(),
                    model.labelRes,
                    model.daysRemaining
                ))
                .setTopButton(model.topButtonRes)
                .setBottomButton(model.bottomButtonRes)

            if (model.email == null) {
                signOutPreference.gone()
            } else {
                signOutPreference.setDesc(model.email)
                signOutPreference.visible()
            }
        })
    }

    private fun setUpNightMode() {
        nightModePreference.setOnClickListener { viewModel.onNightModePreferenceClicked() }

        viewModel.nightMode.observe(this, Observer { mode ->
            val desc = when (mode) {
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM ->
                    getString(R.string.settings_night_mode_follows_system_title)
                AppCompatDelegate.MODE_NIGHT_AUTO ->
                    getString(R.string.settings_night_mode_auto_title)
                AppCompatDelegate.MODE_NIGHT_YES ->
                    getString(R.string.settings_night_mode_yes_title)
                AppCompatDelegate.MODE_NIGHT_NO ->
                    getString(R.string.settings_night_mode_no_title)
                else -> getString(R.string.settings_night_mode_follows_system_title)
            }
            nightModePreference.setDesc(desc)
        })

        viewModel.shouldShowNightModeDialog.observe(this, Observer { event ->
            event.getUnhandledContent()?.let { showNightModeDialog(it) }
        })
    }

    private fun setUpOrientation() {
        orientationPreference.setOnClickListener { viewModel.onOrientationPreferenceClicked() }

        viewModel.orientation.observe(this, Observer {
            orientationPreference.setDesc(getString(it.title))
        })

        viewModel.shouldShowOrientationDialog.observe(this, Observer { event ->
            event.getUnhandledContent()?.let { showOrientationDialog(it) }
        })
    }

    private fun showNightModeDialog(currentMode: Int) {
        NightModeDialog
            .newInstance(currentMode, object : NightModeDialog.NightModeCallback() {
                private var selected: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                override fun onSelected(nightMode: Int) {
                    selected = nightMode
                }

                override fun onDismissed() {
                    viewModel.onNightModeSelected(selected)
                    // TODO Make App observe this instead of calling down.
                    (requireActivity().application as App).updateNightMode()
                }
            })
            .show(requireFragmentManager(), NightModeDialog.TAG)
    }

    private fun showOrientationDialog(currentOrientation: Orientation) {
        OrientationDialog
            .newInstance(currentOrientation, object : OrientationDialog.OrientationCallback() {
                private var selected = Orientation.UNSPECIFIED
                override fun onSelected(orientation: Orientation) {
                    selected = orientation
                }

                override fun onDismissed() {
                    viewModel.onOrientationSelected(selected)
                    // TODO Make App observe this instead of calling down
                    (requireActivity().application as App).updateOrientation()
                }
            })
            .show(activity?.supportFragmentManager, OrientationDialog.TAG)
    }

    override fun onBannerClicked() {
        // Do nothing
    }

    override fun onBannerLabelClicked() {
        // Do nothing
    }

    override fun onBannerTopButtonClicked() {
        viewModel.onBannerTopButtonClicked()
    }

    override fun onBannerBottomButtonClicked() {
        viewModel.onBannerBottomButtonClicked()
    }

    companion object {
        // A tag used for back stack tracking
        const val FRAGMENT_TAG = "settings_fragment_tag"

        const val SUPPORT_EMAIL_ADDRESS = "words@narrate.space"

        fun newInstance() = SettingsFragment()
    }
}
