package com.beeper.lightos

import android.os.Build
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.thelightphone.sdk.EntryPoint
import com.thelightphone.sdk.LightEntryPoint
import com.thelightphone.sdk.shared.LightServerData
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull

@EntryPoint
object BeeperEntryPoint : LightEntryPoint {
    private const val TAG = "BeeperEntryPoint"
    private const val CHANNEL_ID = "beeper_messages"

    override val enablePushNotifications = true

    override suspend fun onToolCreate(serverData: StateFlow<LightServerData?>) {
        Log.d(TAG, "onToolCreate called")
        serverData.filterNotNull().collect { data ->
            data.pushCredentials?.let { creds ->
                Log.d(TAG, "Received UnifiedPush endpoint: ${creds.pushEndpoint}")
                try {
                    BeeperRepository.registerPushEndpoint(creds.pushEndpoint)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to register push endpoint", e)
                }
            }
        }
    }

    override suspend fun onPushNotification(data: ByteArray) {
        val payloadStr = String(data)
        Log.d(TAG, "Received push notification: $payloadStr")
        
        try {
            BeeperRepository.forceBackgroundSync()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling push notification", e)
        }
    }
}
