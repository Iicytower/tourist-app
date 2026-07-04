package com.iicytower.wanderlist.core.constant

import com.iicytower.wanderlist.core.agents.AgentPrompts

object DefaultSettings {
    const val AI_MODEL = "google/gemini-2.5-flash"
    const val DEFAULT_RADIUS_KM = 10
    const val DESCRIPTION_LANGUAGE = "pl"

    val SYSTEM_PROMPT_DESCRIPTION get() = AgentPrompts.description
    val SYSTEM_PROMPT_ASSISTANT get() = AgentPrompts.assistant
}
