package space.narrate.waylan.android.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import space.narrate.waylan.android.R
import space.narrate.waylan.android.databinding.FragmentDetailsBinding
import space.narrate.waylan.android.ui.MainViewModel
import space.narrate.waylan.android.ui.search.SearchFragment
import space.narrate.waylan.android.ui.widget.EducationalOverlayView
import space.narrate.waylan.android.util.BottomSheetCallbackCollection
import space.narrate.waylan.core.data.firestore.users.AddOn
import space.narrate.waylan.core.details.DetailAdapterListener
import space.narrate.waylan.core.details.DetailItemProviderRegistry
import space.narrate.waylan.core.ui.Navigator
import space.narrate.waylan.core.ui.common.SnackbarModel
import space.narrate.waylan.core.ui.widget.ElasticTransition
import space.narrate.waylan.core.util.MathUtils
import space.narrate.waylan.core.util.make

/**
 * A Fragment to show all details of a word (as it appears in the dictionary). This Fragment
 * handles showing the aggregation of WordSet data, Merriam-Webster data and Firestore data
 * for a given word.
 */
class DetailsFragment: Fragment(), DetailAdapterListener {

    private lateinit var binding: FragmentDetailsBinding

    private lateinit var searchFragment: SearchFragment
    // SearchFragment's BottomSheetBehavior
    private val searchSheetBehavior by lazy {
        BottomSheetBehavior.from(searchFragment.requireView())
    }

    private val navigator: Navigator by inject()

    // The MainViewModel which is used to for data shared between MainActivity and
    // its child fragments (HomeFragment, ListFragment and DetailsFragment)
    private val sharedViewModel: MainViewModel by sharedViewModel()

    // This Fragment's ViewModel
    private val viewModel: DetailsViewModel by viewModel()

    private val detailItemProviderRegistry: DetailItemProviderRegistry by inject()
    private val adapter: DetailItemAdapter = DetailItemAdapter(detailItemProviderRegistry, this)

    private val audioClipHelper by lazy {
        AudioClipHelper(requireContext(), this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = ElasticTransition()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Postpone enter transition until we've set everything up.
        postponeEnterTransition()

        searchFragment =
          requireActivity().supportFragmentManager.findFragmentById(R.id.search_fragment) as SearchFragment
        val searchSheetCallback = BottomSheetCallbackCollection()
        var lastState: Int? = null
        var up = true
        searchSheetCallback.addOnStateChangedAction { sheet, newState ->
          if (newState == BottomSheetBehavior.STATE_SETTLING && lastState != BottomSheetBehavior.STATE_SETTLING) {
              println("Calling animateSmear - lastState: $lastState")
              binding.artView.animateSmear(up)
              up = !up
          }
          if (newState != BottomSheetBehavior.STATE_DRAGGING) {
              lastState = newState
          }
//            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
//                binding.artView.animateSmear(true)
//            } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
//                binding.artView.animateSmear(false)
//            }
        }

//        searchSheetCallback.addOnSlideAction { sheet, slideOffset ->
////            binding.artView.smear(slideOffset, true)
//            val sx = MathUtils.normalize(slideOffset, 0F, 1F, 1F, 1.03F)
//            val sy = MathUtils.normalize(slideOffset, 0F, 1F, 1F, 1.03F)
//            val y = MathUtils.normalize(slideOffset, 0F, 1F, 0F, binding.artView.height * .01F)
//            binding.artView.apply {
//                pivotX = width * .2F
//                pivotY = height * .3F
//                scaleX = sx
//                scaleY = sy
//                translationY = -y
//            }
//
//        }

        searchSheetBehavior.addBottomSheetCallback(searchSheetCallback)

        binding.run {

            appBar.doOnElasticDrag(
                alphaViews = listOf(recyclerView, appBar)
            )

            appBar.doOnElasticDismiss {
                navigator.toBack(Navigator.BackType.DRAG, this.javaClass.simpleName)
            }

            appBar.setOnNavigationIconClicked {
                // Child fragments of MainActivity should report how the user is navigating away
                // from them. For more info, see [BaseFragment.setUnconsumedNavigationMethod]
                navigator.toBack(Navigator.BackType.ICON, this.javaClass.simpleName)
            }

            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter
        }

        // Observe the MainViewModel's currentWord. If this changes, it indicates that a user has
        // searched for a different word than is being displayed and this Fragment should
        // react
        sharedViewModel.currentWord.observe(this) {
            binding.appBar.title = it
            viewModel.onCurrentWordChanged(it)
        }

        // Observe all data sources which will be displayed in the [DetailItemAdapter]
        viewModel.list.observe(this) {
            adapter.submitList(it)
        }

        viewModel.shouldShowDragDismissOverlay.observe(this) { event ->
            event.withUnhandledContent {
                EducationalOverlayView.pullDownEducator(binding.appBar).show()
            }
        }

        viewModel.audioClipAction.observe(this) { event ->
            event.withUnhandledContent {
                when (it) {
                    is AudioClipAction.Play -> audioClipHelper.play(it.url)
                    is AudioClipAction.Stop -> audioClipHelper.stop()
                }
            }
        }

        viewModel.shouldShowSnackbar.observe(this) { event ->
            event.withUnhandledContent {
                showSnackbar(it)
            }
        }

        // Start enter transition now that things are set up.
        startPostponedEnterTransition()
    }

    override fun onMwRelatedWordClicked(word: String) {
        sharedViewModel.onChangeCurrentWord(word)
    }

    override fun onMwSuggestionWordClicked(word: String) {
        sharedViewModel.onChangeCurrentWord(word)
    }

    override fun onSynonymChipClicked(synonym: String) {
        sharedViewModel.onChangeCurrentWord(synonym)
    }

    override fun onMwAudioPlayClicked(url: String?) {
        viewModel.onPlayAudioClicked(url)
    }

    override fun onMwAudioStopClicked() {
        viewModel.onStopAudioClicked()
    }

    override fun onMwAudioClipError(messageRes: Int) {
        viewModel.onAudioClipError(messageRes)
    }

    override fun onAddOnDetailsClicked(addOn: AddOn) {
        findNavController()
            .navigate(DetailsFragmentDirections.actionDetailsFragmentToAddOnsFragment(addOn))
    }

    override fun onAddOnDismissClicked(addOn: AddOn) {
        viewModel.onAddOnDismissClicked(addOn)
    }

    override fun onMwThesaurusChipClicked(word: String) {
        sharedViewModel.onChangeCurrentWord(word)
    }

    private fun showSnackbar(model: SnackbarModel) {
        model.make(binding.coordinatorLayout).show()
    }
}

