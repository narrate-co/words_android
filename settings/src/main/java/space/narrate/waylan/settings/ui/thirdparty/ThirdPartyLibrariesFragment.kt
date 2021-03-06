package space.narrate.waylan.settings.ui.thirdparty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import space.narrate.waylan.core.ui.Navigator
import space.narrate.waylan.core.ui.widget.ListItemDividerDecoration
import space.narrate.waylan.core.util.launchWebsite
import space.narrate.waylan.settings.R
import space.narrate.waylan.settings.data.ThirdPartyLibrary
import space.narrate.waylan.settings.databinding.FragmentThirdPartyLibrariesBinding

/**
 * A simple fragment that displays a static list of [ThirdPartyLibrary]
 */
class ThirdPartyLibrariesFragment : Fragment(), ThirdPartyLibraryAdapter.Listener {

    private lateinit var binding: FragmentThirdPartyLibrariesBinding

    private val navigator: Navigator by inject()

    private val thirdPartyLibrariesViewModel: ThirdPartyLibrariesViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)

        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentThirdPartyLibrariesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.run {

            appBar.doOnElasticDrag(
                alphaViews = listOf(recyclerView, appBar)
            )

            appBar.doOnElasticDismiss {
                navigator.toBack(Navigator.BackType.DRAG, this.javaClass.simpleName)
            }

            appBar.setOnNavigationIconClicked {
                navigator.toBack(Navigator.BackType.ICON, this.javaClass.simpleName)
            }

            appBar.setReachableContinuityNavigator(this@ThirdPartyLibrariesFragment, navigator)
        }

        setUpList()
        startPostponedEnterTransition()
    }

    private fun setUpList() {
        val adapter = ThirdPartyLibraryAdapter(this)
        binding.run {
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter

            val itemDivider = ListItemDividerDecoration(
                ContextCompat.getDrawable(requireContext(), R.drawable.list_item_divider)
            )
            recyclerView.addItemDecoration(itemDivider)

        }

        adapter.submitList(thirdPartyLibrariesViewModel.thirdPartyLibraries)
    }

    override fun onClick(lib: ThirdPartyLibrary) { requireContext().launchWebsite(lib.url) }
}