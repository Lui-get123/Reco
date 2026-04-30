package com.example.repartocobro.model

const val EMPANADA_PRICE = 800
const val DEDITO_PRICE = 500

data class Collector(
    val id: Int,
    val name: String
)

data class Route(
    val id: Int,
    val name: String,
    val collectorId: Int
)

data class Store(
    val id: Int,
    val name: String,
    val routeId: Int,
    val deliveredEmpanadas: Int,
    val deliveredDeditos: Int,
    val soldEmpanadas: Int,
    val soldDeditos: Int,
    val isCollected: Boolean,
    val collectionDate: String?,
    val isDelivered: Boolean = false,
    val deliveryDate: String? = null
) {
    val deliveredValue: Int
        get() = deliveredEmpanadas * EMPANADA_PRICE + deliveredDeditos * DEDITO_PRICE

    val collectedValue: Int
        get() = soldEmpanadas * EMPANADA_PRICE + soldDeditos * DEDITO_PRICE
}

data class RouteSummary(
    val stores: List<Store>,
    val totalSoldEmpanadas: Int,
    val totalSoldDeditos: Int,
    val totalCollectedMoney: Int
)

data class LicenseStatus(
    val isActive: Boolean,
    val expirationDate: String?, // formato yyyy-MM-dd
    val daysRemaining: Int
)
