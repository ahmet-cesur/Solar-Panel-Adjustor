package com.acesur.solarpvtracker.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onPurchaseComplete: (Purchase) -> Unit
) {
    companion object {
        const val PRODUCT_ID_30_DAYS = "remove_ads_30_days"
        const val PRODUCT_ID_FOREVER = "removeads"
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i("BillingManager", "onPurchasesUpdated: User canceled the purchase")
        } else {
            Log.e("BillingManager", "onPurchasesUpdated: ${billingResult.debugMessage}")
        }
    }

    private val _billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails: StateFlow<List<ProductDetails>> = _productDetails.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun startConnection() {
        _billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isConnected.value = true
                    queryProductDetails()
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                _isConnected.value = false
                // Implement retry logic here if needed
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_30_DAYS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_FOREVER)
                .setProductType(BillingClient.ProductType.INAPP) // Or SUBS if it's a subscription, but "forever" implies non-consumable INAPP
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        _billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = productDetailsList
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productId: String) {
        val productDetails = _productDetails.value.find { it.productId == productId }
        if (productDetails != null) {
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            )
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()
            _billingClient.launchBillingFlow(activity, billingFlowParams)
        } else {
            Log.e("BillingManager", "Product details not found for $productId")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            
            // Check if it's the 30-day product (consumable)
            var isConsumable = false
            for (productId in purchase.products) {
                if (productId == PRODUCT_ID_30_DAYS) {
                    isConsumable = true
                    break
                }
            }
            
            if (isConsumable) {
                // Consume the 30-day purchase so it can be bought again
                val consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                    
                _billingClient.consumeAsync(consumeParams) { billingResult, _ ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.i("BillingManager", "Purchase consumed")
                        coroutineScope.launch(Dispatchers.Main) {
                            onPurchaseComplete(purchase)
                        }
                    }
                }
            } else if (!purchase.isAcknowledged) {
                // Acknowledge the forever purchase (non-consumable)
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                _billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.i("BillingManager", "Purchase acknowledged")
                        coroutineScope.launch(Dispatchers.Main) {
                            onPurchaseComplete(purchase)
                        }
                    }
                }
            } else {
                // Already acknowledged
                coroutineScope.launch(Dispatchers.Main) {
                    onPurchaseComplete(purchase)
                }
            }
        }
    }
    
    fun queryPurchases() {
        if (!_isConnected.value) return
        
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
            
        _billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
             if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
    }
    
    fun endConnection() {
        _billingClient.endConnection()
    }
}
