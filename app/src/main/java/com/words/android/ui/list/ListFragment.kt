package com.words.android.ui.list

import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.words.android.*
import com.words.android.ui.common.BaseUserFragment
import kotlinx.android.synthetic.main.banner_layout.view.*
import kotlinx.android.synthetic.main.list_fragment.*
import kotlinx.android.synthetic.main.list_fragment.view.*

class ListFragment: BaseUserFragment(), ListTypeAdapter.ListTypeListener {


    enum class ListType(val fragmentTag: String, val title: String) {
        TRENDING("trending_fragment_tag", "Trending"),
        RECENT("recent_fragment_tag", "Recent"),
        FAVORITE("favorite_fragment_tag", "Favorite")
    }

    companion object {
        fun newTrendingInstance(): ListFragment = newInstance(ListType.TRENDING)
        fun newRecentInstance(): ListFragment = newInstance(ListType.RECENT)
        fun newFavoriteInstance(): ListFragment = newInstance(ListType.FAVORITE)

        private fun newInstance(type: ListType): ListFragment {
            val listFrag = ListFragment()
            val args = Bundle()
            args.putString("type", type.name)
            listFrag.arguments = args
            return listFrag
        }
    }

    private val sharedViewModel by lazy {
        ViewModelProviders
                .of(this, viewModelFactory)
                .get(MainViewModel::class.java)
    }

    private val viewModel by lazy {
        ViewModelProviders
                .of(this, viewModelFactory)
                .get(ListViewModel::class.java)
    }

    var type: ListType = ListType.TRENDING

    private val adapter by lazy { ListTypeAdapter(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.list_fragment, container, false)
        type = when (arguments?.getString("type")) {
            ListType.TRENDING.name -> ListType.TRENDING
            ListType.RECENT.name -> ListType.RECENT
            ListType.FAVORITE.name -> ListType.FAVORITE
            else -> ListType.TRENDING
        }
        view.toolbarTitle.text = type.title
        view.navigationIcon.setOnClickListener {
            activity?.onBackPressed()
        }

        setUpBanner(view, type)

        return view
    }

    override fun onEnterTransactionEnded() {
        setUpList()
    }

    private fun setUpBanner(view: View, type: ListType) {
        view.bannerLayout.body.text = when (type) {
            ListType.TRENDING -> getString(R.string.list_banner_trending_body)
            ListType.RECENT -> getString(R.string.list_banner_recents_body)
            ListType.FAVORITE -> getString(R.string.list_banner_favorites_body)
        }
        view.bannerLayout.topButton.setOnClickListener {
            viewModel.setHasSeenBanner(type, true)
        }

        viewModel.getHasSeenBanner(type).observe(this, Observer {
            view.bannerLayout.visibility = if (it) View.GONE else View.VISIBLE
        })
    }

    private fun setUpList() {
        view?.recyclerView?.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        view?.recyclerView?.adapter = adapter
        val itemDivider = ListItemDivider(ContextCompat.getDrawable(context!!, R.drawable.light_list_item_divider))
        view?.recyclerView?.addItemDecoration(itemDivider)

        viewModel.getList(type).observe(this, Observer {
            adapter.submitList(it)
        })
    }

    override fun onWordClicked(word: String) {
        sharedViewModel.setCurrentWordId(word)
        (activity as? MainActivity)?.showDetails()
    }


}