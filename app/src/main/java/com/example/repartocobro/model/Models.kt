package com.example.repartocobro.model

data class Collector(
    val id: Int,
    val name: String
)

data class Route(
    val id: Int,
    val name: String,
    val collectorId: Int
)

data class Product(
    val id: Int,
    val name: String,
    val price: Int,
    val iconName: String? = null
)

data class StoreProduct(
    val storeId: Int,
    val product: Product,
    val deliveredQuantity: Int,
    val soldQuantity: Int
) {
    val deliveredValue: Int get() = deliveredQuantity * product.price
    val collectedValue: Int get() = soldQuantity * product.price
}

data class Store(
    val id: Int,
    val name: String,
    val routeId: Int,
    val products: List<StoreProduct>,
    val collectedStatus: Int, // 0: no cobrado, 1: cobrado, 2: fiado/deuda
    val collectionDate: String?,
    val isDelivered: Boolean = false,
    val deliveryDate: String? = null,
    val observations: String? = null,
    val pendingDebtTotal: Int = 0
) {
    val isCollected: Boolean get() = collectedStatus > 0
    val isFiado: Boolean get() = collectedStatus == 2

    val deliveredValue: Int
        get() = products.sumOf { it.deliveredValue }

    val collectedValue: Int
        get() = if (collectedStatus == 1) products.sumOf { it.collectedValue } else 0
}

data class RouteSummary(
    val stores: List<Store>,
    val totalCollectedMoney: Int,
    val totalPendingDebt: Int = 0
) {
    val totalSoldPerProduct: Map<Product, Int>
        get() = stores.flatMap { it.products }
            .groupBy { it.product }
            .mapValues { entry -> entry.value.sumOf { it.soldQuantity } }
            
    val totalDeliveredPerProduct: Map<Product, Int>
        get() = stores.flatMap { it.products }
            .groupBy { it.product }
            .mapValues { entry -> entry.value.sumOf { it.deliveredQuantity } }
}

data class LicenseStatus(
    val isActive: Boolean,
    val expirationDate: String?, // formato yyyy-MM-dd
    val daysRemaining: Int
)
