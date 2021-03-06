package space.narrate.waylan.core.billing

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener
import space.narrate.waylan.core.data.firestore.users.AddOn
import space.narrate.waylan.core.data.firestore.users.AddOnAction
import space.narrate.waylan.core.data.firestore.users.User
import space.narrate.waylan.core.data.firestore.users.UserAddOnActionUseCase
import space.narrate.waylan.core.repo.UserRepository
import space.narrate.waylan.core.ui.common.Event
import java.util.*

/**
 * A class to handle all communication between Words and Google Play Billing
 *
 * This is a modified version of TrivialDrive 2's BillingManager.
 *
 * Billing works by making a one-time purchase for a plugin, setting the
 * [User.merriamWebsterPurchaseToken] and [User.merriamWebsterStarted] variables in Firebase and
 * then immediately consuming the purchase.
 *
 * The reason for this is because Words plugins are meant to be lightweight purchases that should
 * avoid making the user contemplate a commitment. In an effort to achieve this, Words plugins are
 * one-time purchases that are good for one year (365 days). Once a year elapses, the plugin is
 * expired and [PluginState.isValid]' should return false.
 *
 * @param userRepository Used to update Firestore [User] objects with purchaseToken and
 *      start dates
 *
 * TODO: Possibly break BillingManager out into single use-case specific objects that are created
 * and destroyed with each event.
 **
 */
class BillingManager(
    private val context: Context,
    private val userRepository: UserRepository
): PurchasesUpdatedListener {

    companion object {
        private const val BILLING_MANAGER_NOT_INITIALIZED = -1
        private const val BASE_64_ENCODED_PUBLIC_KEY = "EMPTY_FOR_NOW"
    }

    private val billingClient by lazy {
        BillingClient.newBuilder(context).setListener(this).build()
    }

    private var isServiceConnected = false
    private var billingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED

    private val tokensToBeConsumed = HashSet<String>()

    private val workQueue = PriorityQueue<BillingTask>()

    private val _billingEvent: MutableLiveData<Event<BillingEvent>> = MutableLiveData()
    val billingEvent: LiveData<Event<BillingEvent>>
        get() = _billingEvent

    init {
        doWithServiceConnection {
            queryPurchasesAndSubscription(it, true)
        }
    }

    fun initiatePurchaseFlow(
        activity: Activity,
        addOn: AddOn,
        addOnAction: AddOnAction,
        @BillingClient.SkuType billingType: String = BillingClient.SkuType.INAPP
    ) {
        val sku = if (userRepository.useTestSkus) {
            BillingConfig.TEST_SKU
        } else {
            addOn.sku
        }

        initiatePurchaseFlow(activity, sku, null, billingType)
    }

    private fun initiatePurchaseFlow(
        activity: Activity,
        skuId: String,
        oldSkus: ArrayList<String>?,
        @BillingClient.SkuType billingType: String
    ) {
        doWithServiceConnection {
            val purchaseParams = BillingFlowParams.newBuilder()
                .setSku(skuId)
                .setType(billingType)
                .setOldSkus(oldSkus)
                .build()
            it.launchBillingFlow(activity, purchaseParams)
        }
    }

    fun destroy() {
        if (billingClient != null) {
            billingClient.endConnection()
        }
    }

    fun querySkuDetails(
        @BillingClient.SkuType itemType: String,
        skuList: List<String>,
        listener: SkuDetailsResponseListener
    ) {
        doWithServiceConnection {
            val params = SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(itemType)
            it.querySkuDetailsAsync(params.build()) { responseCode, skuDetailsList ->
                listener.onSkuDetailsResponse(responseCode, skuDetailsList)
            }
        }
    }

    fun consume(purchaseToken: String) {
        if (tokensToBeConsumed.contains(purchaseToken)) {
            return
        }

        tokensToBeConsumed.add(purchaseToken)

        val onConsumeListener = ConsumeResponseListener { _, token ->
            tokensToBeConsumed.remove(token)
        }

        doWithServiceConnection {
            it.consumeAsync(purchaseToken, onConsumeListener)
        }
    }

    private fun areSubscriptionsSupported(): Boolean {
        return billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS) ==
            BillingClient.BillingResponse.OK
    }

    /**
     * Gets all the current active, purchases and subs (non-consumed, non-cancelled, non-expired)
     */
    private fun queryPurchasesAndSubscription(client: BillingClient, consumeAll: Boolean) {
        val purchaseResults = client.queryPurchases(BillingClient.SkuType.INAPP)

        if (areSubscriptionsSupported()) {
            val subscriptionResult = client.queryPurchases(BillingClient.SkuType.SUBS)
            if (subscriptionResult.responseCode == BillingClient.BillingResponse.OK) {
                purchaseResults.purchasesList.addAll(subscriptionResult.purchasesList)
            }
        }

        onQueryPurchasesFinished(purchaseResults, consumeAll)
    }

    override fun onPurchasesUpdated(resultCode: Int, purchases: MutableList<Purchase>?) {
        if (resultCode == BillingClient.BillingResponse.OK) {
            purchases?.forEach {
                handlePurchase(it)
            }
        } else if (resultCode == BillingClient.BillingResponse.USER_CANCELED) {
            // the user canceled the purchase flow
            purchases?.forEach {
                handleCanceled(it)
            }
        } else {
            // a different response was returned
        }
    }

    private fun handleCanceled(purchase: Purchase) {
        when (purchase.sku) {
            BillingConfig.SKU_MERRIAM_WEBSTER -> {
                _billingEvent.value = Event(BillingEvent.Canceled(AddOn.MERRIAM_WEBSTER))
            }
            BillingConfig.SKU_MERRIAM_WEBSTER_THESAURUS -> {
                _billingEvent.value = Event(BillingEvent.Canceled(AddOn.MERRIAM_WEBSTER_THESAURUS))
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        // TODO Make sure we're verifying purchase tokens in a Cloud Function
        // TODO Find a way to pass through the Add-on Action taking place.
        when (purchase.sku) {
            BillingConfig.SKU_MERRIAM_WEBSTER -> {
                userRepository.updateUserAddOn(
                    AddOn.MERRIAM_WEBSTER,
                    UserAddOnActionUseCase.Add(purchase.purchaseToken)
                )
                _billingEvent.value = Event(BillingEvent.Purchased(AddOn.MERRIAM_WEBSTER))
            }
            BillingConfig.SKU_MERRIAM_WEBSTER_THESAURUS -> {
                userRepository.updateUserAddOn(
                    AddOn.MERRIAM_WEBSTER_THESAURUS,
                    UserAddOnActionUseCase.Add(purchase.purchaseToken)
                )
                _billingEvent.value = Event(BillingEvent.Purchased(AddOn.MERRIAM_WEBSTER_THESAURUS))
            }
            // We're using a test sku
            BillingConfig.TEST_SKU,
            BillingConfig.TEST_SKU_PURCHASED,
            BillingConfig.TEST_SKU_CANCELED,
            BillingConfig.TEST_SKU_ITEM_UNAVAILABLE -> {
                // Since there is no way to tell which AddOn belongs to a test sku, update all
                // add ons with the purchase.
                AddOn.values().forEach {
                    userRepository.updateUserAddOn(
                        it,
                        UserAddOnActionUseCase.Add(purchase.purchaseToken)
                    )
                    _billingEvent.value = Event(BillingEvent.Purchased(it))
                }
            }
        }

        consume(purchase.purchaseToken)
    }

    private fun onQueryPurchasesFinished(result: Purchase.PurchasesResult, consumeAll: Boolean) {
        if (billingClient == null || result.responseCode != BillingClient.BillingResponse.OK) {
            return
        }

        if (consumeAll) {
            result.purchasesList.forEach {
                consume(it.purchaseToken)
            }
        } else {
            onPurchasesUpdated(BillingClient.BillingResponse.OK, result.purchasesList)
        }
    }

    private fun doWithServiceConnection(task: (BillingClient) -> Unit) {
        doWithServiceConnection(BillingTask(task))
    }

    private fun doWithServiceConnection(task: BillingTask) {
        // TODO: Replace tasks which have the same id
        workQueue.offer(task)

        if (isServiceConnected) {
            workQueue.pollEach { it.run(billingClient) }
        } else {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(responseCode: Int) {
                    if (responseCode == BillingClient.BillingResponse.OK) {
                        isServiceConnected = true
                        workQueue.pollEach { it.run(billingClient) }
                        billingClientResponseCode = responseCode
                    }
                }
                override fun onBillingServiceDisconnected() {
                    isServiceConnected = false
                }
            })
        }
    }

    /**
     * Copy a queue and immediately clear it. For each item in the copied queue, run [action].
     *
     * This is used to immediately run all queued work to be done with a [BillingClient] and avoid
     * any work being duplicated by subsequent calls to [doWithServiceConnection].
     */
    private fun <E> PriorityQueue<E>.pollEach(action: (E) -> Unit) {
        val items = PriorityQueue(this)
        clear()
        do {
            val item = items.poll()
            if (item != null) action(item)
        } while (items.isNotEmpty())
    }
}