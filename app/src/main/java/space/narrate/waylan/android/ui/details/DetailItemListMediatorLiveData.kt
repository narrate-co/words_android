package space.narrate.waylan.android.ui.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import space.narrate.waylan.core.details.DetailItemModel
import space.narrate.waylan.core.details.DetailItemType

class DetailItemListMediatorLiveData : MediatorLiveData<List<DetailItemModel>>() {

    private var mwModel: DetailItemModel? = null
    private var mwThesaurusModel: DetailItemModel? = null
    private var wordsetModel: DetailItemModel? = null
    private var examplesModel: DetailItemModel? = null

    fun <T : DetailItemModel> addSource(data: LiveData<T>) {
        addSource(data) { set(it) }
    }

    private fun <T : DetailItemModel> set(item: T) {
        when (item.itemType) {
            DetailItemType.MERRIAM_WEBSTER -> {
                if (shouldUpdateList(mwModel, item)) {
                    mwModel = item
                    updateList()
                }
            }
            DetailItemType.MERRIAM_WEBSTER_THESAURUS -> {
                if (shouldUpdateList(mwThesaurusModel, item)) {
                    mwThesaurusModel = item
                    updateList()
                }
            }
            DetailItemType.WORDSET -> {
                if (shouldUpdateList(wordsetModel, item)) {
                    wordsetModel = item
                    updateList()
                }
            }
            DetailItemType.EXAMPLE -> {
                if (shouldUpdateList(examplesModel, item)) {
                    examplesModel = item
                    updateList()
                }
            }
        }
    }

    private fun shouldUpdateList(oldItem: DetailItemModel?, newItem: DetailItemModel): Boolean {
        if (oldItem == null) return true

        return !oldItem.isSameAs(newItem) || !oldItem.isContentSameAs(newItem)
    }

    private fun updateList() {
        postValue(
            listOfNotNull(
                mwModel,
                mwThesaurusModel,
                wordsetModel,
                examplesModel
            ).sortedBy { it.itemType.order }
        )
    }

}