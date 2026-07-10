package com.example.repartocobro.model

const val EMPANADA_PRICE = 1100
const val DEDITO_PRICE = 900

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
        get() = (deliveredEmpanadas * EMPANADA_PRICE) + (deliveredDeditos * DEDITO_PRICE)

    val collectedValue: Int
        get() = if (collectedStatus == 1) (soldEmpanadas * EMPANADA_PRICE) + (soldDeditos * DEDITO_PRICE) else 0
}

data class RouteSummary(
    val stores: List<Store>,
    val totalSoldEmpanadas: Int,
    val totalSoldDeditos: Int,
    val totalCollectedMoney: Int,
    val totalPendingDebt: Int = 0
)

data class LicenseStatus(
    val isActive: Boolean,
    val expirationDate: String?, // formato yyyy-MM-dd
    val daysRemaining: Int
)
