package com.climb.companion.navigation

sealed class CruxRoute(val route: String) {
    data object Welcome : CruxRoute("welcome")
    data object Permissions : CruxRoute("permissions")
    data object Feed : CruxRoute("feed")
}
