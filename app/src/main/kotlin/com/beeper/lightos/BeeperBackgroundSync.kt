package com.beeper.lightos

import com.thelightphone.sdk.LightJob
import com.thelightphone.sdk.LightJobHandler
import com.thelightphone.sdk.LightJobResult

@LightJob("beeper-sync")
val backgroundSyncJob: LightJobHandler = { context, _ ->
    try {
        val appContext = BeeperRepository.appContext
        if (appContext == null) {
            LightJobResult.Error(mapOf("error" to "App not initialized yet. Please open Beeper to start sync."))
        } else {
            BeeperRepository.syncOnce(appContext)
            LightJobResult.Success()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        LightJobResult.Error(mapOf("error" to (e.message ?: "Unknown error")))
    }
}
