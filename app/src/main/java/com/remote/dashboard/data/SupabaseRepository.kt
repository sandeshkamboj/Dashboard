package com.remote.dashboard.data

import android.util.Log
import com.remote.dashboard.model.CommandRequest
import com.remote.dashboard.model.Device
import com.remote.dashboard.model.FileEntry
import com.remote.dashboard.model.LocationEvent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.insert
import io.github.jan.supabase.postgrest.select
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.from as storageFrom
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.signInWith
import io.github.jan.supabase.auth.providers.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.Date

object SupabaseRepository {
    private const val SUPABASE_URL = "YOUR_SUPABASE_URL"
    private const val SUPABASE_KEY = "YOUR_SUPABASE_ANON_KEY"
    private const val EMAIL = "your_email@example.com"
    private const val PASSWORD = "your_password"
    private var client: SupabaseClient? = null

    suspend fun ensureLogin() {
        if (client != null) return
        client = createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Auth)
            install(Storage)
        }
        try {
            client?.auth?.signInWith(Email) {
                email = EMAIL
                password = PASSWORD
            }
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Login failed: $e")
        }
    }

    suspend fun fetchDevices(): List<Device> {
        ensureLogin()
        return try {
            val result = client?.from("devices")?.select()
            val deviceList = mutableListOf<Device>()
            if (result is List<*>) {
                result.forEach { row ->
                    if (row is Map<*, *>) {
                        val androidId = row["android_id"] as? String
                        val name = row["device_name"] as? String
                        val lastSeen = row["last_seen"] as? String
                        if (!androidId.isNullOrBlank()) {
                            deviceList.add(Device(androidId, name ?: androidId, lastSeen))
                        }
                    }
                }
            }
            deviceList
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "fetchDevices failed: $e")
            emptyList()
        }
    }

    suspend fun sendCommand(request: CommandRequest): Boolean {
        ensureLogin()
        return try {
            client?.from("commands")?.insert(
                mapOf(
                    "device_id" to request.deviceId,
                    "type" to request.type,
                    "options" to request.options
                )
            )
            true
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Send command failed: $e")
            false
        }
    }

    suspend fun fetchFiles(bucket: String): List<FileEntry> {
        ensureLogin()
        return try {
            val files = client?.storageFrom(bucket)?.list()
            files?.map {
                FileEntry(
                    name = it.name,
                    path = it.name,
                    size = it.metadata?.get("size")?.toString()?.toLongOrNull() ?: 0L,
                    type = bucket,
                    lastModified = 0L
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "fetchFiles failed: $e")
            emptyList()
        }
    }

    suspend fun fetchLocations(): List<LocationEvent> {
        val entries = fetchFiles("location")
        return entries.mapNotNull {
            val bytes = downloadFile("location", it.path)
            try {
                val json = bytes.decodeToString()
                val obj = Json.parseToJsonElement(json).jsonObject
                LocationEvent(
                    lat = obj["lat"]?.toString()?.toDoubleOrNull() ?: 0.0,
                    lng = obj["lng"]?.toString()?.toDoubleOrNull() ?: 0.0,
                    timestamp = obj["timestamp"]?.toString()?.toLongOrNull() ?: 0L
                )
            } catch (_: Exception) { null }
        }
    }

    suspend fun downloadFile(bucket: String, path: String): ByteArray {
        ensureLogin()
        return withContext(Dispatchers.IO) {
            try {
                client?.storageFrom(bucket)?.download(path) ?: ByteArray(0)
            } catch (e: Exception) {
                Log.e("SupabaseRepo", "Download failed: $e")
                ByteArray(0)
            }
        }
    }

    suspend fun deleteFile(bucket: String, path: String): Boolean {
        ensureLogin()
        return try {
            client?.storageFrom(bucket)?.delete(path)
            true
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Delete failed: $e")
            false
        }
    }

    suspend fun uploadFile(bucket: String, fileName: String, bytes: ByteArray): Boolean {
        ensureLogin()
        return try {
            client?.storageFrom(bucket)?.upload(fileName, bytes, upsert = true)
            true
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Upload failed: $e")
            false
        }
    }
}