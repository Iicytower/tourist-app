package com.iicytower.wanderlist.core.agents

object AgentPrompts {
    var qualityFilter: String = ""
        private set
    var description: String = ""
        private set
    var assistant: String = ""
        private set
    var tripPlan: String = ""
        private set
    var objectInfo: String = ""
        private set

    fun init(loader: (String) -> String) {
        qualityFilter = loader("quality-filter")
        description = loader("description")
        assistant = loader("assistant")
        tripPlan = loader("trip-plan")
        objectInfo = loader("object-info")
    }
}
