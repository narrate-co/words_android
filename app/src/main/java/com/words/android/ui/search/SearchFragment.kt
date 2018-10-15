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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.words.android.*
import com.words.android.databinding.SearchFragmentBinding
import com.words.android.ui.common.BaseUserFragment
import com.words.android.util.hideSoftKeyboard

class SearchFragment : BaseUserFragment(), WordsAdapter.WordAdapterHandlers {

    companion object {
        fun newInstance() = SearchFragment()
    }

    private val viewModel by lazy {
        ViewModelProviders
                .of(this, viewModelFactory)
                .get(SearchViewModel::class.java)
    }

    private val sharedViewHolder by lazy {
        ViewModelProviders
                .of(this, viewModelFactory)
                .get(MainViewModel::class.java)
    }

    private val bottomSheetBehavior by lazy {
        BottomSheetBehavior.from(view)
    }

    private val adapter by lazy { WordsAdapter(this) }

    private var hideKeyboard = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: SearchFragmentBinding = DataBindingUtil.inflate(inflater, R.layout.search_fragment, container, false)
        binding.setLifecycleOwner(this)
        binding.viewModel = viewModel

        //set up recycler view
        binding.recycler.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.recycler.adapter = adapter

        binding.searchEditText.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED || bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }


        viewModel.searchResults.observe(this, Observer {
            adapter.submitList(it)
        })

        return binding.root
    }

    override fun onWordClicked(word: String) {
        sharedViewHolder.setCurrentWordId(word)
        hideKeyboard = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        activity?.hideSoftKeyboard()
        (activity as MainActivity).showDetails()
    }

}

