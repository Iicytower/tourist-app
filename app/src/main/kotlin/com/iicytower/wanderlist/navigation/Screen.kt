package com.iicytower.wanderlist.navigation

sealed class Screen(val route: String) {
    object Search : Screen("search")
    object Map : Screen("map?lat={lat}&lon={lon}&xid={xid}") {
        fun createRoute(lat: Double, lon: Double, xid: String) = "map?lat=$lat&lon=$lon&xid=$xid"
        const val baseRoute = "map"
    }
    object MyList : Screen("mylist")
    object Assistant : Screen("assistant")
    object Settings : Screen("settings")
    object AttractionDetail : Screen("attraction/{xid}") {
        fun createRoute(xid: String) = "attraction/$xid"
    }
}
