package jp.lagado.literaryfragments

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * iOS StoreKit チケット消耗型と同型の Play Billing 台帳。
 * 型の参考: AbomonLicenseManager（別アプリ・別 Product ID）。
 * 有料枚数は PocketFortunePrefs.remainingTickets、無料日次枠とは分離。
 * purchaseToken 単位で冪等 · acknowledge 後に consume。
 */
class TicketLicenseManager(
    context: Context,
    private val onStatusChanged: ((Status) -> Unit)? = null,
) : PurchasesUpdatedListener {

    data class Status(
        val paidTickets: Int,
        val prices: Map<String, String>,
        val phase: String,
        val ok: Boolean,
        val message: String? = null,
    )

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_LICENSE, Context.MODE_PRIVATE)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val tag = "FragmentsTickets"
    private val purchaseInProgress = AtomicBoolean(false)
    private var activityRef: WeakReference<Activity>? = null

    private var productDetailsById: Map<String, ProductDetails> = emptyMap()
    private val cachedPrices = mutableMapOf<String, String>()
    private var pendingPurchaseCallback: ((Status) -> Unit)? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .enableAutoServiceReconnection()
        .build()

    fun attachActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun start() {
        connectBilling()
    }

    fun destroy() {
        try {
            billingClient.endConnection()
        } catch (_: Exception) {
        }
    }

    fun currentStatus(phase: String = "status", ok: Boolean = true, message: String? = null): Status {
        return Status(
            paidTickets = TicketManager.getPaidTickets(appContext),
            prices = displayPrices(),
            phase = phase,
            ok = ok,
            message = message,
        )
    }

    /** Play 未取得時も iOS `fallbackPriceLabel` と同型で目安価格を出す */
    fun displayPrice(productId: String): String {
        cachedPrices[productId]?.let { return it }
        return FALLBACK_PRICES[productId] ?: "—"
    }

    /** Debug は開発付与可。Release は Play の ProductDetails が取れたときだけ購入可 */
    fun isProductAvailable(productId: String): Boolean {
        if (BuildConfig.DEBUG) return ticketAmount(productId) != null
        return productDetailsById.containsKey(productId)
    }

    fun purchase(productId: String, onResult: (Status) -> Unit) {
        if (ticketAmount(productId) == null) {
            onResult(currentStatus("product_unavailable", false))
            return
        }
        if (!purchaseInProgress.compareAndSet(false, true)) {
            onResult(currentStatus("error", false, "purchase_in_progress"))
            return
        }
        pendingPurchaseCallback = { status ->
            purchaseInProgress.set(false)
            pendingPurchaseCallback = null
            onResult(status)
        }

        // USB Debug は Play 未経由のため本課金シートが失敗しやすい → 開発付与のみ
        if (BuildConfig.DEBUG) {
            Log.d(tag, "DEBUG purchase → development grant ($productId)")
            grantDevelopmentPurchaseOrFail(productId)
            return
        }

        if (!billingClient.isReady) {
            connectBilling()
            finishPending(currentStatus("product_unavailable", false))
            return
        }

        refreshProductDetails {
            val details = productDetailsById[productId]
            if (details == null) {
                finishPending(currentStatus("product_unavailable", false))
                return@refreshProductDetails
            }
            val activity = activityRef?.get()
            if (activity == null || activity.isFinishing) {
                finishPending(currentStatus("error", false, "no_activity"))
                return@refreshProductDetails
            }
            val offer = details.oneTimePurchaseOfferDetails
            if (offer == null) {
                finishPending(currentStatus("product_unavailable", false))
                return@refreshProductDetails
            }
            val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
            val offerToken = offer.offerToken
            if (!offerToken.isNullOrBlank()) {
                productParamsBuilder.setOfferToken(offerToken)
            }
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
                .build()
            val launch = billingClient.launchBillingFlow(activity, flowParams)
            if (launch.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(tag, "launchBillingFlow: ${launch.debugMessage}")
                finishPending(currentStatus("error", false, launch.debugMessage))
            }
        }
    }

    private fun displayPrices(): Map<String, String> {
        val out = linkedMapOf<String, String>()
        ALL_PRODUCTS.forEach { id ->
            out[id] = displayPrice(id)
        }
        return out
    }

    private fun connectBilling() {
        if (billingClient.isReady) {
            refreshProductDetails(null)
            queryAndProcessOwnedPurchases()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(tag, "billing ready")
                    refreshProductDetails(null)
                    queryAndProcessOwnedPurchases()
                } else {
                    Log.w(tag, "billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(tag, "billing disconnected")
            }
        })
    }

    private fun refreshProductDetails(done: (() -> Unit)?) {
        if (!billingClient.isReady) {
            done?.invoke()
            return
        }
        val products = ALL_PRODUCTS.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        billingClient.queryProductDetailsAsync(params) { result, queryProductDetailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val detailsList = queryProductDetailsResult.productDetailsList
                productDetailsById = detailsList.associateBy { it.productId }
                detailsList.forEach { d ->
                    d.oneTimePurchaseOfferDetails?.formattedPrice?.let { price ->
                        cachedPrices[d.productId] = price
                    }
                }
                val unfetched = queryProductDetailsResult.unfetchedProductList
                if (unfetched.isNotEmpty()) {
                    Log.w(tag, "unfetched products: ${unfetched.map { it.productId }}")
                }
                Log.d(tag, "products loaded: ${productDetailsById.keys}")
                mainHandler.post {
                    onStatusChanged?.invoke(currentStatus("prices", true))
                }
            } else {
                Log.w(tag, "queryProductDetails: ${result.debugMessage}")
            }
            mainHandler.post { done?.invoke() }
        }
    }

    private fun grantDevelopmentPurchaseOrFail(productId: String) {
        val amount = ticketAmount(productId)
        if (amount == null) {
            finishPending(currentStatus("product_unavailable", false))
            return
        }
        val token = "dev-${productId}-${UUID.randomUUID()}"
        if (!recordTransactionAndGrant(token, productId)) {
            finishPending(currentStatus("error", false, "persistence_failed"))
            return
        }
        finishPending(currentStatus("purchased", true))
    }

    private fun finishPending(status: Status) {
        val cb = pendingPurchaseCallback
        pendingPurchaseCallback = null
        purchaseInProgress.set(false)
        mainHandler.post {
            cb?.invoke(status)
            onStatusChanged?.invoke(status)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    if (pendingPurchaseCallback != null) {
                        finishPending(currentStatus("error", false, "empty_purchase"))
                    }
                    return
                }
                purchases.forEach { purchase ->
                    processPurchase(purchase) { ok ->
                        if (pendingPurchaseCallback != null) {
                            if (ok) {
                                finishPending(currentStatus("purchased", true))
                            } else {
                                finishPending(currentStatus("error", false, "persistence_failed"))
                            }
                        } else {
                            onStatusChanged?.invoke(currentStatus())
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                if (pendingPurchaseCallback != null) {
                    finishPending(currentStatus("cancelled", false))
                }
            }
            else -> {
                Log.w(tag, "onPurchasesUpdated: ${result.debugMessage}")
                if (pendingPurchaseCallback != null) {
                    finishPending(currentStatus("error", false, result.debugMessage))
                }
            }
        }
    }

    private fun processPurchase(purchase: Purchase, done: (Boolean) -> Unit) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            done(false)
            return
        }
        val productId = purchase.products.firstOrNull() ?: run {
            done(false)
            return
        }
        if (ticketAmount(productId) == null) {
            done(false)
            return
        }
        val token = purchase.purchaseToken
        if (!recordTransactionAndGrant(token, productId)) {
            done(false)
            return
        }
        fun consume() {
            val params = ConsumeParams.newBuilder().setPurchaseToken(token).build()
            billingClient.consumeAsync(params) { br, _ ->
                if (br.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(tag, "consume: ${br.debugMessage}")
                }
                mainHandler.post { done(true) }
            }
        }
        if (purchase.isAcknowledged) {
            consume()
        } else {
            val ack = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()
            billingClient.acknowledgePurchase(ack) { br ->
                if (br.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(tag, "acknowledge: ${br.debugMessage}")
                }
                consume()
            }
        }
    }

    private fun queryAndProcessOwnedPurchases() {
        if (!billingClient.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            purchases.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    processPurchase(purchase) { /* background sync */ }
                }
            }
        }
    }

    /** purchaseToken 既知なら枚数は足さない（冪等）。新規なら足して台帳保存。 */
    private fun recordTransactionAndGrant(token: String, productId: String): Boolean {
        val amount = ticketAmount(productId) ?: return false
        val ledger = loadLedger()
        if (ledger.transactions.containsKey(token)) {
            return true
        }
        ledger.transactions[token] = productId
        if (!persistLedger(ledger)) return false
        return TicketManager.addPaidTickets(appContext, amount)
    }

    private data class Ledger(
        val transactions: MutableMap<String, String>,
    )

    private fun loadLedger(): Ledger {
        val raw = prefs.getString(KEY_LEDGER, null)
        if (raw.isNullOrBlank()) return Ledger(mutableMapOf())
        return try {
            val o = JSONObject(raw)
            val tx = mutableMapOf<String, String>()
            val arr = o.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val e = arr.getJSONObject(i)
                tx[e.getString("id")] = e.getString("productId")
            }
            Ledger(tx)
        } catch (_: Exception) {
            Ledger(mutableMapOf())
        }
    }

    private fun persistLedger(ledger: Ledger): Boolean {
        return try {
            val arr = JSONArray()
            ledger.transactions.forEach { (id, pid) ->
                arr.put(JSONObject().put("id", id).put("productId", pid))
            }
            prefs.edit().putString(KEY_LEDGER, JSONObject().put("transactions", arr).toString()).apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        // 余裕多め: 100/500/1000 · Console 価格 ¥300 / ¥900 / ¥1,500（¥250は価格帯に無い）
        const val PRODUCT_100 = "jp.lagado.literaryfragments.ticket100"
        const val PRODUCT_500 = "jp.lagado.literaryfragments.ticket500"
        const val PRODUCT_1000 = "jp.lagado.literaryfragments.ticket1000"

        val ALL_PRODUCTS = listOf(PRODUCT_100, PRODUCT_500, PRODUCT_1000)

        /** Console 未反映時の表示用（iOS IAPProduct.fallbackPriceLabel と同型） */
        private val FALLBACK_PRICES = mapOf(
            PRODUCT_100 to "¥300",
            PRODUCT_500 to "¥900",
            PRODUCT_1000 to "¥1,500",
        )

        private const val PREFS_LICENSE = "fragments_ticket_license"
        private const val KEY_LEDGER = "purchaseLedger.v1"

        fun ticketAmount(productId: String): Int? = when (productId) {
            PRODUCT_100 -> 100
            PRODUCT_500 -> 500
            PRODUCT_1000 -> 1000
            else -> null
        }
    }
}
