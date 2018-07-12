package com.words.android.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.words.android.App
import com.words.android.MainActivity
import com.words.android.MainViewModel
import com.words.android.R
import com.words.android.databinding.SearchFragmentBinding
import com.words.android.data.repository.Word


class SearchFragment : Fragment(), WordsAdapter.WordAdapterHandlers {

    companion object {
        fun newInstance() = SearchFragment()
    }

    private val viewModel by lazy {
        ViewModelProviders
                .of(this, (activity?.application as App).viewModelFactory)
                .get(SearchViewModel::class.java)
    }

    private val sharedViewHolder by lazy {
        ViewModelProviders
                .of(activity!!, (activity?.application as App).viewModelFactory)
                .get(MainViewModel::class.java)
    }

    private val bottomSheetBehavior by lazy {
        BottomSheetBehavior.from(view)
    }

    private val adapter by lazy { WordsAdapter(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: SearchFragmentBinding = DataBindingUtil.inflate(inflater, R.layout.search_fragment, container, false)
        binding.setLifecycleOwner(this)
        binding.viewModel = viewModel

        //set up recycler view
        binding.recycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.recycler.adapter = adapter

        viewModel.searchResults.observe(this, Observer {
            if (it != null) {
                println("search results changed. size = ${it.size}")
                if (it.isEmpty()) setPeekHighMin() else setPeekHighMax()
                adapter.submitList(it)
            }
        })

        return binding.root
    }

    override fun onWordClicked(word: Word) {
        sharedViewHolder.setCurrentWordId(word.dbWord?.word ?: "")
        setPeekHighMin()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        (activity as MainActivity).showDetails()
    }

    private fun setPeekHighMin() {
        bottomSheetBehavior.peekHeight = resources.getDimensionPixelOffset(R.dimen.search_min_peek_height)
    }

    private fun setPeekHighMax() {
        bottomSheetBehavior.peekHeight = resources.getDimensionPixelOffset(R.dimen.search_max_peek_height)
    }

}

