package com.iicytower.wanderlist.navigation

sealed class Screen(val route: String) {
    object Search : Screen("search")
    object Map : Screen("map?lat={lat}&lon={lon}&xid={xid}") {
        fun createRoute(lat: Double, lon: Double, xid: String) = "map?lat=$lat&lon=$lon&xid=$xid"
        const val baseRoute = "map"
    }
    object MyList : Screen("mylist")
    object Assistant : Screen("assistant?listId={listId}") {
        fun createRoute(listId: Long? = null) = if (listId != null) "assistant?listId=$listId" else "assistant"
        const val baseRoute = "assistant"
    }
    object Settings : Screen("settings")
    object TripListDetail : Screen("triplist/{listId}") {
        fun createRoute(listId: Long) = "triplist/$listId"
    }
    object TripPlan : Screen("tripplan/{listId}?listName={listName}") {
        fun createRoute(listId: Long, listName: String) = "tripplan/$listId?listName=${listName}"
    }
    object AttractionDetail : Screen("attraction/{xid}") {
        fun createRoute(xid: String) = "attraction/$xid"
    }
}
